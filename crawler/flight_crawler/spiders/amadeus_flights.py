from datetime import date, timedelta
from pathlib import Path

import scrapy

from flight_crawler.amadeus_client import fetch_flight_offers
from flight_crawler.parser import parse_flights


class AmadeusFlightsSpider(scrapy.Spider):
    name = "amadeus_flights"

    def __init__(
        self,
        from_city="上海",
        to_city="北京",
        date=None,
        adults="1",
        max_results="5",
        *args,
        **kwargs,
    ):
        super().__init__(*args, **kwargs)
        self.from_city = from_city
        self.to_city = to_city
        self.date = date or (date_today() + timedelta(days=7)).isoformat()
        self.adults = int(adults)
        self.max_results = int(max_results)
        self.source = "amadeus"
        self.request_params = (
            f"source=amadeus, fromCity={self.from_city}, toCity={self.to_city}, "
            f"date={self.date}, adults={self.adults}, maxResults={self.max_results}"
        )

    async def start(self):
        for flight in self._collect_flights():
            yield flight

    def _collect_flights(self):
        try:
            flights = fetch_flight_offers(
                from_city=self.from_city,
                to_city=self.to_city,
                date=self.date,
                adults=self.adults,
                max_results=self.max_results,
            )
            if flights:
                return flights
            self.logger.warning("Amadeus returned no rows or credentials are missing; using sample fallback.")
        except Exception as exc:
            self.logger.warning("Amadeus collection failed; using sample fallback: %s", exc)
        return self._sample_flights()

    def _sample_flights(self):
        sample_path = Path(__file__).resolve().parents[2] / "sample_pages" / "flights.html"
        return parse_flights(sample_path.read_text(encoding="utf-8"))


def date_today():
    return date.today()
