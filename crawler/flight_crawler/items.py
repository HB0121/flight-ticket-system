"""
Scrapy Item 定义模块。

FlightItem 是整个系统三层架构中 crawler 层的数据传输对象（DTO）。
每个 FlightItem 实例代表一条航班记录，由 Spider yield 后经过 Pipeline 写入 MySQL。

Item 字段与数据库 flight 表字段一一对应，表结构定义见：
  - infra/mysql/init.sql
  - backend/src/main/java/com/example/flight/config/DatabaseInitializer.java
"""

import scrapy


class FlightItem(scrapy.Item):
    """航班数据实体（Scrapy Item）

    每个字段对应 flight 表中的一列。
    Field() 中可以传入 dict 作为元数据（如 serializer），此处使用默认空参数。
    Pipeline 通过 item["field_name"] 方式读写各个字段。
    """

    # 航班号，如 MU5101、CA1234
    flight_no = scrapy.Field()

    # 航空公司中文全称，如 "东方航空"、"中国国航"
    airline_name = scrapy.Field()

    # 出发城市中文名，如 "上海"
    from_city = scrapy.Field()

    # 到达城市中文名，如 "北京"
    to_city = scrapy.Field()

    # 出发机场中文名，如 "浦东机场"
    from_airport = scrapy.Field()

    # 到达机场中文名，如 "首都机场"
    to_airport = scrapy.Field()

    # 出发时间，ISO 8601 格式字符串，如 "2025-06-01T08:00:00"
    depart_time = scrapy.Field()

    # 到达时间，ISO 8601 格式字符串
    arrive_time = scrapy.Field()

    # 票价，整数（元），如 1280
    price = scrapy.Field()

    # 剩余座位数，整数，如 42
    seats_left = scrapy.Field()

    # 数据来源标识，枚举值："sample" 或 "amadeus"
    # 用于前端区分数据来源、数据库去重键的一部分
    data_source = scrapy.Field()
