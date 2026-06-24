from urllib.parse import parse_qs, urlparse

from flight_crawler.live_sources import SiteAdapter, provider_from_source
from flight_crawler.robots_guard import RobotsGuard
from flight_crawler.spiders.live_flights import error_for_status


def test_site_adapter_builds_predictable_ctrip_url():
    url = SiteAdapter("ctrip").build_url("上海", "北京", "2026-06-19")
    parsed = urlparse(url)

    assert parsed.scheme == "https"
    assert parsed.netloc == "flights.ctrip.com"
    assert parsed.path == "/online/channel/domestic"
    assert parse_qs(parsed.query) == {
        "from": ["上海"],
        "to": ["北京"],
        "date": ["2026-06-19"],
    }


def test_site_adapter_builds_predictable_fliggy_and_qunar_urls():
    fliggy_url = SiteAdapter("fliggy").build_url("上海", "北京", "2026-06-19")
    qunar_url = SiteAdapter("qunar").build_url("上海", "北京", "2026-06-19")

    assert parse_qs(urlparse(fliggy_url).query) == {
        "from": ["上海"],
        "to": ["北京"],
        "date": ["2026-06-19"],
    }
    assert parse_qs(urlparse(qunar_url).query) == {
        "searchDepartureAirport": ["上海"],
        "searchArrivalAirport": ["北京"],
        "searchDepartureTime": ["2026-06-19"],
    }


def test_provider_from_source_maps_live_backend_sources():
    assert provider_from_source("ctrip_live") == "ctrip"
    assert provider_from_source("fliggy_live") == "fliggy"
    assert provider_from_source("qunar_live") == "qunar"


def test_robots_guard_blocks_disallowed_paths_without_fetching_target_page():
    calls = []

    class Response:
        text = "User-agent: *\nDisallow: /blocked\n"

        def raise_for_status(self):
            return None

    def fetch(url, **_kwargs):
        calls.append(url)
        return Response()

    allowed, reason = RobotsGuard(fetch=fetch).can_fetch("https://example.com/blocked/search")

    assert allowed is False
    assert reason == "ROBOTS_DISALLOWED"
    assert calls == ["https://example.com/robots.txt"]


def test_live_spider_maps_blocking_http_statuses_to_clear_errors():
    assert error_for_status(401) == "LOGIN_REQUIRED"
    assert error_for_status(403) == "LOGIN_REQUIRED"
    assert error_for_status(429) == "CAPTCHA_OR_RISK_CONTROL"
    assert error_for_status(432) == "CAPTCHA_OR_RISK_CONTROL"
    assert error_for_status(500) == "HTTP_STATUS_500"
