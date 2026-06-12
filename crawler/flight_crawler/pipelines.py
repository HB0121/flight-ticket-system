import os
from datetime import datetime

import pymysql


class MysqlPipeline:
    def open_spider(self, spider):
        self.connection = pymysql.connect(
            host=os.getenv("MYSQL_HOST", "127.0.0.1"),
            port=int(os.getenv("MYSQL_PORT", "3306")),
            user=os.getenv("MYSQL_USER", "flight"),
            password=os.getenv("MYSQL_PASSWORD", "flight123"),
            database=os.getenv("MYSQL_DATABASE", "flight_demo"),
            charset="utf8mb4",
            autocommit=False,
        )
        self._ensure_schema()
        self.success_count = 0
        self.failed_count = 0
        self.source = getattr(spider, "source", getattr(spider, "name", "sample"))
        self.actual_sources = set()
        self.request_params = getattr(spider, "request_params", f"source={self.source}")
        with self.connection.cursor() as cursor:
            cursor.execute(
                """
                insert into crawl_job(
                    status, started_at, success_count, failed_count,
                    source, actual_source, fallback_reason, request_params
                )
                values (%s, %s, %s, %s, %s, %s, %s, %s)
                """,
                ("RUNNING", datetime.now(), 0, 0, self.source, self.source, None, self.request_params),
            )
            self.job_id = cursor.lastrowid
        self.connection.commit()

    def process_item(self, item, spider):
        try:
            with self.connection.cursor() as cursor:
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
                        item["depart_time"].replace("T", " "),
                        item["arrive_time"].replace("T", " "),
                        item["price"],
                        item["seats_left"],
                        item["data_source"],
                        datetime.now(),
                    ),
                )
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
            self.success_count += 1
            if item.get("data_source"):
                self.actual_sources.add(item["data_source"])
            self.connection.commit()
        except Exception:
            self.failed_count += 1
            self.connection.rollback()
            raise
        return item

    def close_spider(self, spider):
        status = "SUCCESS" if spider.crawler.stats.get_value("finish_reason") == "finished" else "FAILED"
        error_message = None if status == "SUCCESS" else str(spider.crawler.stats.get_value("finish_reason"))
        actual_source = self._resolve_actual_source(spider)
        fallback_reason = getattr(spider, "fallback_reason", None)
        with self.connection.cursor() as cursor:
            cursor.execute(
                """
                update crawl_job
                set status = %s,
                    finished_at = %s,
                    success_count = %s,
                    failed_count = %s,
                    error_message = %s,
                    actual_source = %s,
                    fallback_reason = %s
                where id = %s
                """,
                (
                    status,
                    datetime.now(),
                    self.success_count,
                    self.failed_count,
                    error_message,
                    actual_source,
                    fallback_reason,
                    self.job_id,
                ),
            )
        self.connection.commit()
        self.connection.close()

    def _ensure_schema(self):
        with self.connection.cursor() as cursor:
            for statement in [
                "alter table crawl_job add column source varchar(32) null",
                "alter table crawl_job add column actual_source varchar(32) null",
                "alter table crawl_job add column fallback_reason varchar(500) null",
                "alter table crawl_job add column request_params varchar(500) null",
            ]:
                try:
                    cursor.execute(statement)
                except Exception:
                    pass
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

    def _resolve_actual_source(self, spider):
        if len(self.actual_sources) == 1:
            return next(iter(self.actual_sources))
        if len(self.actual_sources) > 1:
            return "mixed"
        return getattr(spider, "actual_source", self.source)
