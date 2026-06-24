from datetime import date as date_type
from datetime import datetime, timedelta

import scrapy

from flight_crawler.aerodatabox_client import fetch_airport_flights


class AeroDataBoxFlightsSpider(scrapy.Spider):
    name = "aerodatabox_flights"

    def __init__(
        self,
        airport_code=None,
        date=None,
        source="aerodatabox",
        *args,
        **kwargs,
    ):
        super().__init__(*args, **kwargs)
        self.airport_code = (airport_code or "").strip().upper()
        self.date = date or (today() + timedelta(days=7)).isoformat()
        self.source = "aerodatabox"
        self.request_params = (
            f"source={self.source}, airportCode={self.airport_code}, date={self.date}"
        )
        self._validate_inputs(source)

    async def start(self):
        for flight in self._collect_flights():
            yield flight

    def _collect_flights(self):
        try:
            return fetch_airport_flights(
                airport_code=self.airport_code,
                target_date=self.date,
            )
        except Exception as exc:
            self.crawler.stats.set_value("crawl_error", str(exc))
            raise RuntimeError(f"AeroDataBox sync failed: {exc}") from exc

    def _validate_inputs(self, source):
        if source and source.strip().lower() not in {"aerodatabox", "amadeus"}:
            raise ValueError(f"unsupported source: {source}")
        if len(self.airport_code) != 3:
            raise ValueError("airport_code must be a 3-letter IATA code")
        try:
            datetime.strptime(self.date, "%Y-%m-%d")
        except ValueError as exc:
            raise ValueError("date must use YYYY-MM-DD format") from exc


def today() -> date_type:
    return date_type.today()
