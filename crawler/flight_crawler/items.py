import scrapy


class FlightItem(scrapy.Item):
    flight_no = scrapy.Field()
    airline_name = scrapy.Field()
    from_city = scrapy.Field()
    to_city = scrapy.Field()
    from_airport = scrapy.Field()
    to_airport = scrapy.Field()
    depart_time = scrapy.Field()
    arrive_time = scrapy.Field()
    price = scrapy.Field()
    seats_left = scrapy.Field()
    data_source = scrapy.Field()

