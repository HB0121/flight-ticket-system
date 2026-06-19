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
        self.source = getattr(spider, "source", getattr(spider, "name", "unknown"))
        self.request_params = getattr(spider, "request_params", f"source={self.source}")
        with self.connection.cursor() as cursor:
            cursor.execute(
                """
                insert into crawl_job(status, started_at, success_count, failed_count, source, request_params)
                values (%s, %s, %s, %s, %s, %s)
                """,
                ("RUNNING", datetime.now(), 0, 0, self.source, self.request_params),
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
            self.connection.commit()
        except Exception:
            self.failed_count += 1
            self.connection.rollback()
            raise
        return item

    def close_spider(self, spider):
        finish_reason = spider.crawler.stats.get_value("finish_reason")
        status = self.close_status(self.success_count, self.failed_count, finish_reason)
        live_error = spider.crawler.stats.get_value("live_error")
        crawl_error = spider.crawler.stats.get_value("crawl_error")
        if getattr(spider, "name", "") == "live_flights" and self.success_count == 0:
            status = "FAILED"
            live_error = live_error or "NO_FLIGHT_ROWS_PARSED"
        if crawl_error:
            status = "FAILED"
        error_message = None if status == "SUCCESS" else str(crawl_error or live_error or finish_reason)
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

    @staticmethod
    def close_status(success_count, failed_count, finish_reason):
        if failed_count > 0:
            return "FAILED"
        if success_count > 0 or finish_reason == "finished":
            return "SUCCESS"
        return "FAILED"

    def _ensure_schema(self):
        with self.connection.cursor() as cursor:
            for statement in [
                "alter table crawl_job add column source varchar(32) null",
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
