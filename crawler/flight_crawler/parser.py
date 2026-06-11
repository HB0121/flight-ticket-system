import re
from bs4 import BeautifulSoup


def parse_flights(html: str) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    flights = []

    for card in soup.select(".flight-card"):
        flight = {
            "flight_no": card.get("data-flight-no", "").strip(),
            "airline_name": _text(card, ".airline"),
            "from_city": _text(card, ".from-city"),
            "to_city": _text(card, ".to-city"),
            "from_airport": _text(card, ".from-airport"),
            "to_airport": _text(card, ".to-airport"),
            "depart_time": _attr(card, ".depart-time", "datetime"),
            "arrive_time": _attr(card, ".arrive-time", "datetime"),
            "price": _number(_text(card, ".price")),
            "seats_left": _number(_text(card, ".seats-left")),
            "data_source": "sample",
        }
        if _is_complete(flight):
            flights.append(flight)

    return flights


def _text(card, selector: str) -> str:
    node = card.select_one(selector)
    return node.get_text(strip=True) if node else ""


def _attr(card, selector: str, attr: str) -> str:
    node = card.select_one(selector)
    return node.get(attr, "").strip() if node else ""


def _number(value: str) -> int:
    match = re.search(r"\d+", value.replace(",", ""))
    return int(match.group(0)) if match else 0


def _is_complete(flight: dict) -> bool:
    required = [
        "flight_no",
        "airline_name",
        "from_city",
        "to_city",
        "from_airport",
        "to_airport",
        "depart_time",
        "arrive_time",
    ]
    return all(flight.get(key) for key in required) and flight["price"] > 0

