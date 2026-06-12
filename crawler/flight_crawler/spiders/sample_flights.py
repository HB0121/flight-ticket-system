from pathlib import Path

import scrapy

from flight_crawler.parser import parse_flights


class SampleFlightsSpider(scrapy.Spider):
    name = "sample_flights"
    source = "sample"
    actual_source = "sample"
    fallback_reason = None

    async def start(self):
        for request in self.start_requests():
            yield request

    def start_requests(self):
        sample_path = Path(__file__).resolve().parents[2] / "sample_pages" / "flights.html"
        yield scrapy.Request(sample_path.as_uri(), callback=self.parse)

    def parse(self, response):
        for flight in parse_flights(response.text):
            yield flight
