import hashlib
import json
import logging
import os
from datetime import date as date_type

import requests


logger = logging.getLogger(__name__)
BASE_URL = "https://aerodatabox.p.rapidapi.com"
HOST = "aerodatabox.p.rapidapi.com"
QUERY_PARAMS = {
    "withLeg": "true",
    "direction": "Both",
    "withCancelled": "true",
    "withCodeshared": "true",
    "withCargo": "false",
    "withPrivate": "false",
    "withLocation": "false",
}


def build_time_windows(target_date: str | date_type) -> list[tuple[str, str]]:
    day = target_date.isoformat() if isinstance(target_date, date_type) else str(target_date)
    return [
        (f"{day}T00:00", f"{day}T12:00"),
        (f"{day}T12:00", f"{day}T23:59"),
    ]


def build_dedupe_key(flight: dict) -> str:
    return "|".join([
        flight.get("flight_no", ""),
        flight.get("depart_time", ""),
        flight.get("from_airport", ""),
        flight.get("to_airport", ""),
    ])


def generate_simulated_price(flight_no: str, depart_time: str, from_airport: str, to_airport: str) -> int:
    key = f"{flight_no}|{depart_time}|{from_airport}|{to_airport}|price"
    return 500 + stable_mod(key, 1001)


def generate_simulated_seats(flight_no: str, depart_time: str, from_airport: str, to_airport: str) -> int:
    key = f"{flight_no}|{depart_time}|{from_airport}|{to_airport}|seats"
    return 3 + stable_mod(key, 28)


def fetch_airport_flights(
    airport_code: str,
    target_date: str,
    api_key: str | None = None,
    base_url: str | None = None,
    host: str | None = None,
    session=None,
) -> list[dict]:
    api_key = api_key or os.getenv("AERODATABOX_KEY")
    if not api_key or not api_key.strip():
        raise ValueError("AERODATABOX_KEY is missing")

    base_url = (base_url or os.getenv("AERODATABOX_BASE_URL") or BASE_URL).rstrip("/")
    host = host or os.getenv("AERODATABOX_HOST") or HOST
    client = session or requests
    rows: list[dict] = []
    seen: set[str] = set()
    normalized_airport = airport_code.strip().upper()

    for from_local, to_local in build_time_windows(target_date):
        payload = fetch_airport_window(client, base_url, host, api_key, normalized_airport, from_local, to_local)
        for direction, flights in (
                ("departures", safe_list(safe_get(payload, "departures"))),
                ("arrivals", safe_list(safe_get(payload, "arrivals")))):
            for flight in flights:
                row = normalize_flight(flight, normalized_airport, direction)
                if not row:
                    continue
                dedupe_key = build_dedupe_key(row)
                if dedupe_key in seen:
                    continue
                seen.add(dedupe_key)
                rows.append(row)
    return rows


def fetch_airport_window(client, base_url: str, host: str, api_key: str, airport_code: str, from_local: str, to_local: str) -> dict:
    response = client.get(
        f"{base_url}/flights/airports/iata/{airport_code}/{from_local}/{to_local}",
        headers={
            "X-RapidAPI-Key": api_key,
            "X-RapidAPI-Host": host,
        },
        params=QUERY_PARAMS,
        timeout=20,
    )
    if response.status_code >= 400:
        body = safe_response_text(response)
        raise RuntimeError(f"AeroDataBox request failed [{response.status_code}]: {body}")
    return parse_json_payload(response)


def normalize_flight(flight: dict, airport_code: str, direction: str) -> dict | None:
    if not isinstance(flight, dict):
        logger.warning("Skip non-dict flight row from AeroDataBox: %r", flight)
        return None

    departure = safe_dict(safe_get(flight, "departure"))
    arrival = safe_dict(safe_get(flight, "arrival"))
    departure_airport = airport_data(departure, "airport")
    arrival_airport = airport_data(arrival, "airport")

    from_airport = airport_iata(departure_airport) or (airport_code if direction == "departures" else "")
    to_airport = airport_iata(arrival_airport) or (airport_code if direction == "arrivals" else "")
    depart_time = scheduled_local(departure)
    arrive_time = scheduled_local(arrival)

    if direction == "departures":
        from_airport = from_airport or airport_code
    if direction == "arrivals":
        to_airport = to_airport or airport_code

    flight_no = flight_number(flight)
    if not all([flight_no, from_airport, to_airport, depart_time, arrive_time]):
        return None

    airline_name = airline_display_name(flight)
    from_city = airport_city_name(departure_airport) or from_airport
    to_city = airport_city_name(arrival_airport) or to_airport
    price = generate_simulated_price(flight_no, depart_time, from_airport, to_airport)
    seats_left = generate_simulated_seats(flight_no, depart_time, from_airport, to_airport)

    return {
        "flight_no": flight_no,
        "airline_name": airline_name,
        "from_city": from_city,
        "to_city": to_city,
        "from_airport": from_airport,
        "to_airport": to_airport,
        "depart_time": normalize_time(depart_time),
        "arrive_time": normalize_time(arrive_time),
        "price": price,
        "seats_left": seats_left,
        "data_source": "aerodatabox",
        "terminal": first_text(safe_get(departure, "terminal"), safe_get(arrival, "terminal")),
        "status": first_text(safe_get(flight, "status")),
        "aircraft": aircraft_name(flight),
    }


def aircraft_name(flight: dict) -> str:
    aircraft = safe_dict(safe_get(flight, "aircraft"))
    model = safe_dict(safe_get(aircraft, "model"))
    return first_text(safe_get(model, "text"), safe_get(model, "name"), safe_get(aircraft, "modelCode"), "")


def airline_display_name(flight: dict) -> str:
    airline = safe_dict(safe_get(flight, "airline"))
    return first_text(safe_get(airline, "name"), safe_get(airline, "shortName"), safe_get(airline, "iata"), "Unknown Airline")


def flight_number(flight: dict) -> str:
    codeshared = safe_dict(safe_get(flight, "codeshared"))
    return first_text(
        safe_get(flight, "number"),
        safe_get(flight, "callSign"),
        safe_get(codeshared, "number"),
        safe_get(codeshared, "callSign"),
        "",
    )


def airport_data(section: dict, key: str) -> dict:
    value = safe_get(section, key)
    return value if isinstance(value, dict) else {}


def airport_iata(airport: dict) -> str:
    return first_text(safe_get(airport, "iata"), safe_get(airport, "iataCode"), safe_get(airport, "name"), "").upper()


def airport_city_name(airport: dict) -> str:
    return first_text(
        safe_get(airport, "municipalityName"),
        safe_get(airport, "city"),
        safe_get(airport, "shortName"),
        safe_get(airport, "name"),
        airport_iata(airport),
    )


def scheduled_local(section: dict) -> str:
    scheduled_time = safe_dict(safe_get(section, "scheduledTime"))
    if not scheduled_time:
        raw_time = safe_get(section, "scheduledTime")
        return first_text(raw_time, safe_get(section, "scheduledTimeLocal"), "")
    return first_text(
        safe_get(scheduled_time, "local"),
        safe_get(scheduled_time, "utc"),
        safe_get(section, "scheduledTimeLocal"),
        "",
    )


def normalize_time(value: str) -> str:
    text = value.strip()
    if text.endswith("Z"):
        text = text[:-1]
    if len(text) == 16:
        text = text + ":00"
    if len(text) >= 19:
        return text[:19]
    return text


def first_text(*values) -> str:
    for value in values:
        if isinstance(value, str) and value.strip():
            return value.strip()
    return ""


def stable_mod(value: str, modulo: int) -> int:
    digest = hashlib.sha256(value.encode("utf-8")).hexdigest()
    return int(digest[:12], 16) % modulo


def parse_json_payload(response) -> dict:
    try:
        payload = response.json()
    except ValueError:
        payload = json.loads(getattr(response, "text", "") or "{}")
    if not isinstance(payload, dict):
        logger.warning("AeroDataBox returned non-dict payload: %r", payload)
        return {}
    return payload


def safe_get(value, key, default=""):
    if not isinstance(value, dict):
        return default
    return value.get(key, default)


def safe_dict(value) -> dict:
    return value if isinstance(value, dict) else {}


def safe_list(value) -> list:
    return value if isinstance(value, list) else []


def safe_response_text(response) -> str:
    text = getattr(response, "text", "") or ""
    return text.strip()[:300] or "no response body"
