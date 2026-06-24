from datetime import date as date_type, timedelta

import scrapy
from scrapy.exceptions import CloseSpider

from flight_crawler.live_parser import parse_live_flights
from flight_crawler.live_sources import SiteAdapter
from flight_crawler.robots_guard import RobotsGuard


class LiveFlightsSpider(scrapy.Spider):
    name = "live_flights"
    custom_settings = {
        "CONCURRENT_REQUESTS_PER_DOMAIN": 1,
        "DOWNLOAD_DELAY": 2,
        "USER_AGENT": "flight-learning-crawler/0.1",
    }

    def __init__(
        self,
        provider,
        from_city="上海",
        to_city="北京",
        date=None,
        adults="1",
        max_results="5",
        *args,
        **kwargs,
    ):
        super().__init__(*args, **kwargs)
        self.provider = provider
        self.from_city = from_city
        self.to_city = to_city
        self.date = date or (date_type.today() + timedelta(days=7)).isoformat()
        self.adults = int(adults)
        self.max_results = int(max_results)
        self.adapter = SiteAdapter(provider)
        self.source = self.adapter.data_source
        self.start_url = self.adapter.build_url(self.from_city, self.to_city, self.date)
        self.request_params = (
            f"source={self.source}, provider={self.provider}, fromCity={self.from_city}, "
            f"toCity={self.to_city}, date={self.date}, adults={self.adults}, "
            f"maxResults={self.max_results}"
        )

    async def start(self):
        allowed, reason = RobotsGuard().can_fetch(self.start_url)
        if not allowed:
            self._fail(reason)
        yield scrapy.Request(
            self.start_url,
            callback=self.parse,
            dont_filter=True,
            meta={"handle_httpstatus_all": True},
        )

    def parse(self, response):
        if response.status >= 400:
            self._fail(error_for_status(response.status))
        flights, error = parse_live_flights(
            response.text,
            data_source=self.source,
            from_city=self.from_city,
            to_city=self.to_city,
            date=self.date,
            max_results=self.max_results,
        )
        if error:
            self._fail(error)
        for flight in flights:
            yield flight

    def _fail(self, reason):
        self.crawler.stats.set_value("live_error", reason)
        raise CloseSpider(reason)


def error_for_status(status):
    if status in (401, 403):
        return "LOGIN_REQUIRED"
    if status in (429, 432):
        return "CAPTCHA_OR_RISK_CONTROL"
    return f"HTTP_STATUS_{status}"
