from flight_crawler.aerodatabox_client import (
    build_dedupe_key,
    build_time_windows,
    fetch_airport_flights,
    generate_simulated_price,
    generate_simulated_seats,
    normalize_flight,
)
from flight_crawler.parser import parse_flights


def test_parse_flights_from_html_fixture():
    html = """
    <main>
      <article class="flight-card" data-flight-no="MU5101">
        <span class="airline">China Eastern</span>
        <span class="from-city">Shanghai</span>
        <span class="to-city">Beijing</span>
        <span class="from-airport">PVG</span>
        <span class="to-airport">PEK</span>
        <time class="depart-time" datetime="2026-06-19T08:30:00">08:30</time>
        <time class="arrive-time" datetime="2026-06-19T10:45:00">10:45</time>
        <strong class="price">980</strong>
        <span class="seats-left">12</span>
      </article>
    </main>
    """

    flights = parse_flights(html, data_source="fixture_html")

    assert flights == [
        {
            "flight_no": "MU5101",
            "airline_name": "China Eastern",
            "from_city": "Shanghai",
            "to_city": "Beijing",
            "from_airport": "PVG",
            "to_airport": "PEK",
            "depart_time": "2026-06-19T08:30:00",
            "arrive_time": "2026-06-19T10:45:00",
            "price": 980,
            "seats_left": 12,
            "data_source": "fixture_html",
        }
    ]


def test_build_time_windows_splits_one_day_into_two_requests():
    assert build_time_windows("2026-06-18") == [
        ("2026-06-18T00:00", "2026-06-18T12:00"),
        ("2026-06-18T12:00", "2026-06-18T23:59"),
    ]


def test_normalize_departure_maps_current_airport_as_origin():
    flight = normalize_flight(
        {
            "number": "MU5101",
            "status": "Scheduled",
            "airline": {"name": "China Eastern"},
            "departure": {
                "scheduledTime": {"local": "2026-06-19T08:30"},
                "terminal": "T1",
                "airport": {"iata": "PVG", "municipalityName": "Shanghai"},
            },
            "arrival": {
                "scheduledTime": {"local": "2026-06-19T10:45"},
                "airport": {"iata": "PEK", "municipalityName": "Beijing"},
            },
            "aircraft": {"model": {"text": "Airbus A320"}},
        },
        "PVG",
        "departures",
    )

    assert flight["flight_no"] == "MU5101"
    assert flight["from_airport"] == "PVG"
    assert flight["to_airport"] == "PEK"
    assert flight["from_city"] == "Shanghai"
    assert flight["to_city"] == "Beijing"
    assert flight["depart_time"] == "2026-06-19T08:30:00"
    assert flight["arrive_time"] == "2026-06-19T10:45:00"
    assert flight["data_source"] == "aerodatabox"


def test_normalize_arrival_maps_current_airport_as_destination():
    flight = normalize_flight(
        {
            "number": "CA1234",
            "airline": {"name": "Air China"},
            "departure": {
                "scheduledTime": {"local": "2026-06-19T07:00"},
                "airport": {"iata": "SHA", "municipalityName": "Shanghai"},
            },
            "arrival": {
                "scheduledTime": {"local": "2026-06-19T09:10"},
                "airport": {"municipalityName": "Chongqing"},
            },
        },
        "CKG",
        "arrivals",
    )

    assert flight["from_airport"] == "SHA"
    assert flight["to_airport"] == "CKG"
    assert flight["from_city"] == "Shanghai"
    assert flight["to_city"] == "Chongqing"


def test_simulated_price_and_seats_are_stable_for_same_flight():
    price1 = generate_simulated_price("MU5101", "2026-06-19T08:30:00", "PVG", "PEK")
    price2 = generate_simulated_price("MU5101", "2026-06-19T08:30:00", "PVG", "PEK")
    seats1 = generate_simulated_seats("MU5101", "2026-06-19T08:30:00", "PVG", "PEK")
    seats2 = generate_simulated_seats("MU5101", "2026-06-19T08:30:00", "PVG", "PEK")

    assert price1 == price2
    assert 500 <= price1 <= 1500
    assert seats1 == seats2
    assert 3 <= seats1 <= 30


def test_build_dedupe_key_matches_required_fields():
    flight = {
        "flight_no": "MU5101",
        "depart_time": "2026-06-19T08:30:00",
        "from_airport": "PVG",
        "to_airport": "PEK",
    }

    assert build_dedupe_key(flight) == "MU5101|2026-06-19T08:30:00|PVG|PEK"


def test_normalize_flight_skips_string_rows_instead_of_calling_get():
    assert normalize_flight("bad-row", "CKG", "departures") is None


def test_normalize_flight_handles_string_nested_fields():
    flight = normalize_flight(
        {
            "number": "MU5102",
            "airline": "China Eastern",
            "departure": {
                "scheduledTime": "2026-06-19T08:30",
                "airport": "PVG",
            },
            "arrival": {
                "scheduledTime": {"local": "2026-06-19T10:45"},
                "airport": {"iata": "PEK", "name": "Beijing Capital"},
            },
            "aircraft": "A320",
        },
        "PVG",
        "departures",
    )

    assert flight["flight_no"] == "MU5102"
    assert flight["from_airport"] == "PVG"
    assert flight["to_airport"] == "PEK"
    assert flight["from_city"] == "PVG"
    assert flight["airline_name"] == "Unknown Airline"


def test_fetch_airport_flights_ignores_non_dict_payload_and_rows():
    class DummyResponse:
        status_code = 200
        text = '"bad-payload"'

        def __init__(self, payload):
            self._payload = payload

        def json(self):
            return self._payload

    class DummyClient:
        def __init__(self):
            self.calls = 0

        def get(self, *args, **kwargs):
            self.calls += 1
            if self.calls == 1:
                return DummyResponse("bad-payload")
            return DummyResponse({
                "departures": ["bad-row"],
                "arrivals": [{
                    "number": "CA1234",
                    "departure": {
                        "scheduledTime": {"local": "2026-06-19T07:00"},
                        "airport": {"iata": "SHA", "municipalityName": "Shanghai"},
                    },
                    "arrival": {
                        "scheduledTime": {"local": "2026-06-19T09:10"},
                        "airport": {"iata": "CKG", "municipalityName": "Chongqing"},
                    },
                    "airline": {"name": "Air China"},
                }],
            })

    flights = fetch_airport_flights(
        airport_code="CKG",
        target_date="2026-06-19",
        api_key="test-key",
        session=DummyClient(),
    )

    assert len(flights) == 1
    assert flights[0]["flight_no"] == "CA1234"
