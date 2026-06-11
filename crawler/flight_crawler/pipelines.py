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
        self.success_count = 0
        self.failed_count = 0
        with self.connection.cursor() as cursor:
            cursor.execute(
                """
                insert into crawl_job(status, started_at, success_count, failed_count)
                values (%s, %s, %s, %s)
                """,
                ("RUNNING", datetime.now(), 0, 0),
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
            self.success_count += 1
            self.connection.commit()
        except Exception:
            self.failed_count += 1
            self.connection.rollback()
            raise
        return item

    def close_spider(self, spider):
        status = "SUCCESS" if spider.crawler.stats.get_value("finish_reason") == "finished" else "FAILED"
        error_message = None if status == "SUCCESS" else str(spider.crawler.stats.get_value("finish_reason"))
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

