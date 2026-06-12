"""
Scrapy Item Pipeline：MySQL 数据持久化模块。

MysqlPipeline 是 Scrapy 管线架构的核心组件，负责将爬取的
航班数据写入 MySQL 数据库。它实现了 Scrapy Pipeline 的三个
标准生命周期方法：

  open_spider  ──→  建立连接 + 确保表结构 + 创建 crawl_job 记录
  process_item ──→  逐条 upsert flight + insert price_snapshot
  close_spider ──→  更新 crawl_job 状态 + 关闭连接

数据写入策略：
- flight 表：INSERT ... ON DUPLICATE KEY UPDATE（幂等 upsert）
  唯一键为 (flight_no, depart_time, data_source)
- flight_price_snapshot 表：追加写入，每次爬取都会新增一条快照记录
- crawl_job 表：记录每次爬取任务的源、参数、成功/失败计数

Schema 演进：
- _ensure_schema() 在每次爬取启动时执行轻量级迁移（ALTER + CREATE IF NOT EXISTS）
  保证即使数据库是旧的，也能通过添加列和表来适配新版本代码。
  这与后端 DatabaseInitializer.java 和 infra/mysql/init.sql 保持同步。
"""

import os
from datetime import datetime

import pymysql


class MysqlPipeline:
    """MySQL 数据管道

    负责将 FlightItem 数据持久化到 MySQL 的三张表中：
    flight（当前快照）、flight_price_snapshot（历史价格）、crawl_job（审计日志）。

    连接参数全部从环境变量读取，方便 Docker 和本地运行两种场景。
    """

    # ==================== 生命周期方法 ====================

    def open_spider(self, spider):
        """Spider 启动时调用：建立数据库连接并初始化。

        执行步骤：
        1. 从环境变量读取 MySQL 连接参数，建立 pymysql 连接
        2. 调用 _ensure_schema() 确保表结构为最新
        3. 初始化成功/失败计数器
        4. 从 spider 实例上读取 source 和 request_params（用于审计）
        5. 创建一条 status=RUNNING 的 crawl_job 记录，并记录其 id

        参数:
            spider: Scrapy Spider 实例，需包含 source 和 request_params 属性
        """
        # 读取连接参数，全部支持环境变量覆盖，提供默认值用于本地开发
        self.connection = pymysql.connect(
            host=os.getenv("MYSQL_HOST", "127.0.0.1"),
            port=int(os.getenv("MYSQL_PORT", "3306")),
            user=os.getenv("MYSQL_USER", "flight"),
            password=os.getenv("MYSQL_PASSWORD", "flight123"),
            database=os.getenv("MYSQL_DATABASE", "flight_demo"),
            charset="utf8mb4",
            autocommit=False,  # 手动管理事务，确保每条 item 原子写入
        )
        # 确保表结构兼容当前版本（轻量级迁移）
        self._ensure_schema()

        # 初始化统计计数器
        self.success_count = 0
        self.failed_count = 0

        # 数据来源标识：优先取 spider.source，其次回退到 spider.name
        self.source = getattr(spider, "source", getattr(spider, "name", "sample"))
        # 请求参数（用于审计记录）：如 "source=amadeus, fromCity=上海, toCity=北京, ..."
        self.request_params = getattr(spider, "request_params", f"source={self.source}")

        # 创建一条 RUNNING 状态的爬取任务记录，后续在 close_spider 中更新结果
        with self.connection.cursor() as cursor:
            cursor.execute(
                """
                insert into crawl_job(status, started_at, success_count, failed_count, source, request_params)
                values (%s, %s, %s, %s, %s, %s)
                """,
                ("RUNNING", datetime.now(), 0, 0, self.source, self.request_params),
            )
            # 获取自增主键 id，后续 close_spider 时需要据此更新状态
            self.job_id = cursor.lastrowid
        self.connection.commit()

    def process_item(self, item, spider):
        """处理单个 Item：写入 flight 和 flight_price_snapshot 两张表。

        写入策略（事务内两步）：
        1. Upsert flight 表：使用 INSERT ... ON DUPLICATE KEY UPDATE
           - 唯一键：(flight_no, depart_time, data_source)
           - 新航班 → 插入；已有航班 → 仅更新可变字段（价格、余座等）
        2. Insert flight_price_snapshot 表：追加一条历史快照
           - 每次爬取都会为每个航班新增一条记录，形成价格时间序列

        参数:
            item: FlightItem 实例（或其 dict）
            spider: Scrapy Spider 实例

        返回:
            item: 原样返回，供下游 Pipeline 继续处理

        异常处理：
            任一步骤失败都会 rollback 当前事务，
            failed_count +1，然后重新抛出异常。
        """
        try:
            with self.connection.cursor() as cursor:
                # ---------- 步骤 1: Upsert flight 表 ----------
                # depart_time/arrive_time 中的 "T" 替换为空格以适配 MySQL datetime 格式
                cursor.execute(
                    """
                    insert into flight(
                        flight_no, airline_name, from_city, to_city, from_airport, to_airport,
                        depart_time, arrive_time, price, seats_left, data_source, collected_at
                    )
                    values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    on duplicate key update
                        airline_name = values(airline_name),
                        from_city = values(from_city),
                        to_city = values(to_city),
                        from_airport = values(from_airport),
                        to_airport = values(to_airport),
                        arrive_time = values(arrive_time),
                        price = values(price),
                        seats_left = values(seats_left),
                        collected_at = values(collected_at)
                    """,
                    (
                        item["flight_no"],
                        item["airline_name"],
                        item["from_city"],
                        item["to_city"],
                        item["from_airport"],
                        item["to_airport"],
                        # ISO 格式 T 替换为空格，适配 MySQL datetime 列
                        item["depart_time"].replace("T", " "),
                        item["arrive_time"].replace("T", " "),
                        item["price"],
                        item["seats_left"],
                        item["data_source"],
                        datetime.now(),
                    ),
                )

                # ---------- 步骤 2: 查询 flight 表主键 id ----------
                # 需要 flight_id 作为 snapshot 表的外键
                cursor.execute(
                    """
                    select id from flight
                    where flight_no = %s and depart_time = %s and data_source = %s
                    """,
                    (
                        item["flight_no"],
                        item["depart_time"].replace("T", " "),
                        item["data_source"],
                    ),
                )
                flight_id = cursor.fetchone()[0]

                # ---------- 步骤 3: Insert flight_price_snapshot 表 ----------
                # 追加写入，每次爬取产生新的快照行，形成历史价格曲线
                cursor.execute(
                    """
                    insert into flight_price_snapshot(
                        flight_id, flight_no, from_city, to_city, depart_time,
                        price, seats_left, data_source, observed_at
                    )
                    values (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    (
                        flight_id,
                        item["flight_no"],
                        item["from_city"],
                        item["to_city"],
                        item["depart_time"].replace("T", " "),
                        item["price"],
                        item["seats_left"],
                        item["data_source"],
                        datetime.now(),
                    ),
                )

            # 事务提交：两条写入全部成功才算一个 item 处理完成
            self.success_count += 1
            self.connection.commit()
        except Exception:
            # 任意异常：回滚事务（避免部分写入），增加失败计数，继续抛出
            self.failed_count += 1
            self.connection.rollback()
            raise
        return item

    def close_spider(self, spider):
        """Spider 关闭时调用：更新爬取任务状态并断开连接。

        根据 Scrapy 的 finish_reason 来判断爬取是否成功完成：
        - "finished" → status = SUCCESS
        - 其他（如 "shutdown"、"item_error" 等）→ status = FAILED

        参数:
            spider: Scrapy Spider 实例
        """
        # 根据爬虫结束原因判断成功与否
        status = "SUCCESS" if spider.crawler.stats.get_value("finish_reason") == "finished" else "FAILED"
        # 失败时记录错误信息
        error_message = None if status == "SUCCESS" else str(spider.crawler.stats.get_value("finish_reason"))

        # 更新 crawl_job 表的最终状态、结束时间、成功/失败计数
        with self.connection.cursor() as cursor:
            cursor.execute(
                """
                update crawl_job
                set status = %s, finished_at = %s, success_count = %s, failed_count = %s, error_message = %s
                where id = %s
                """,
                (status, datetime.now(), self.success_count, self.failed_count, error_message, self.job_id),
            )
        self.connection.commit()
        self.connection.close()

    # ==================== Schema 迁移 ====================

    def _ensure_schema(self):
        """确保数据库表结构为最新版本（轻量级迁移）。

        执行以下操作（均幂等，重复执行不报错）：
        1. 为 crawl_job 表添加 source 和 request_params 列（如果还不存在）
           ALTER TABLE 失败时（列已存在）静默忽略
        2. 创建 flight_price_snapshot 表（如果还不存在）
           CREATE TABLE IF NOT EXISTS 保证幂等

        这种设计使得即使 infra/mysql/init.sql 尚未执行，
        crawler 也能自动创建必要的表结构。

        注意：此处应与后端 DatabaseInitializer.java 和
        infra/mysql/init.sql 保持列定义一致。
        """
        with self.connection.cursor() as cursor:
            # 阶段 1：为旧版本 crawl_job 表添加审计字段（幂等）
            for statement in [
                "alter table crawl_job add column source varchar(32) null",
                "alter table crawl_job add column request_params varchar(500) null",
            ]:
                try:
                    cursor.execute(statement)
                except Exception:
                    # 列已存在时 ALTER 会报错，忽略即可
                    pass

            # 阶段 2：创建价格快照表（幂等）
            cursor.execute(
                """
                create table if not exists flight_price_snapshot (
                    id bigint primary key auto_increment,
                    flight_id bigint not null,
                    flight_no varchar(20) not null,
                    from_city varchar(32) not null,
                    to_city varchar(32) not null,
                    depart_time datetime not null,
                    price decimal(10, 2) not null,
                    seats_left int not null default 0,
                    data_source varchar(32) not null,
                    observed_at datetime not null,
                    key idx_snapshot_flight (flight_id, observed_at),
                    key idx_snapshot_route (from_city, to_city, depart_time)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
                """
            )
        self.connection.commit()
