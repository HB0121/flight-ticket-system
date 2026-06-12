from flight_crawler.parser import parse_flights
from flight_crawler.amadeus_client import (
    city_to_iata,
    normalize_flight_offers,
    should_use_sample_fallback,
)
from flight_crawler.spiders.amadeus_flights import AmadeusFlightsSpider


def test_parse_flights_from_sample_html():
    html = """
    <main>
      <article class="flight-card" data-flight-no="MU5101">
        <span class="airline">东方航空</span>
        <span class="from-city">上海</span>
        <span class="to-city">北京</span>
        <span class="from-airport">虹桥机场</span>
        <span class="to-airport">首都机场</span>
        <time class="depart-time" datetime="2026-06-19T08:30:00">08:30</time>
        <time class="arrive-time" datetime="2026-06-19T10:45:00">10:45</time>
        <strong class="price">980</strong>
        <span class="seats-left">12</span>
      </article>
    </main>
    """

    flights = parse_flights(html)

    assert flights == [
        {
            "flight_no": "MU5101",
            "airline_name": "东方航空",
            "from_city": "上海",
            "to_city": "北京",
            "from_airport": "虹桥机场",
            "to_airport": "首都机场",
            "depart_time": "2026-06-19T08:30:00",
            "arrive_time": "2026-06-19T10:45:00",
            "price": 980,
            "seats_left": 12,
            "data_source": "sample",
        }
    ]


def test_normalize_amadeus_flight_offers_to_internal_rows():
    response = {
        "data": [
            {
                "numberOfBookableSeats": 9,
                "price": {"total": "866.40"},
                "validatingAirlineCodes": ["MU"],
                "itineraries": [
                    {
                        "segments": [
                            {
                                "carrierCode": "MU",
                                "number": "5101",
                                "departure": {"iataCode": "PVG", "at": "2026-06-19T08:30:00"},
                                "arrival": {"iataCode": "PEK", "at": "2026-06-19T10:45:00"},
                            }
                        ]
                    }
                ],
            }
        ]
    }

    flights = normalize_flight_offers(response, from_city="上海", to_city="北京")

    assert flights == [
        {
            "flight_no": "MU5101",
            "airline_name": "东方航空",
            "from_city": "上海",
            "to_city": "北京",
            "from_airport": "浦东机场",
            "to_airport": "首都机场",
            "depart_time": "2026-06-19T08:30:00",
            "arrive_time": "2026-06-19T10:45:00",
            "price": 866,
            "seats_left": 9,
            "data_source": "amadeus",
        }
    ]


def test_amadeus_missing_credentials_uses_sample_fallback():
    assert should_use_sample_fallback("", "") is True
    assert should_use_sample_fallback("client-id", "secret") is False
    assert city_to_iata("上海") == "SHA"


def test_amadeus_spider_marks_actual_source_when_falling_back(monkeypatch):
    def fake_fetch_flight_offers(**_kwargs):
        return []

    monkeypatch.setattr(
        "flight_crawler.spiders.amadeus_flights.fetch_flight_offers",
        fake_fetch_flight_offers,
    )
    spider = AmadeusFlightsSpider(from_city="上海", to_city="北京", date="2026-06-19")

    flights = spider._collect_flights()

    assert flights
    assert {flight["data_source"] for flight in flights} == {"sample"}
    assert spider.actual_source == "sample"
    assert "Amadeus returned no rows" in spider.fallback_reason
