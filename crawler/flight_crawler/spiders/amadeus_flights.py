from flight_crawler.spiders.aerodatabox_flights import AeroDataBoxFlightsSpider


class AmadeusFlightsSpider(AeroDataBoxFlightsSpider):
    name = "amadeus_flights"
