import os
from decimal import Decimal, ROUND_HALF_UP

import requests


CITY_IATA = {
    "北京": "BJS",
    "上海": "SHA",
    "广州": "CAN",
    "深圳": "SZX",
    "成都": "CTU",
    "杭州": "HGH",
    "西安": "SIA",
    "重庆": "CKG",
    "武汉": "WUH",
    "南京": "NKG",
    "厦门": "XMN",
    "青岛": "TAO",
}

AIRPORT_NAMES = {
    "PEK": "首都机场",
    "PKX": "大兴机场",
    "PVG": "浦东机场",
    "SHA": "虹桥机场",
    "CAN": "白云机场",
    "SZX": "宝安机场",
    "CTU": "双流机场",
    "TFU": "天府机场",
    "HGH": "萧山机场",
    "XIY": "咸阳机场",
    "CKG": "江北机场",
    "WUH": "天河机场",
    "NKG": "禄口机场",
    "XMN": "高崎机场",
    "TAO": "胶东机场",
}

AIRLINE_NAMES = {
    "CA": "中国国航",
    "MU": "东方航空",
    "CZ": "南方航空",
    "HU": "海南航空",
    "ZH": "深圳航空",
    "MF": "厦门航空",
    "3U": "四川航空",
}


def city_to_iata(city: str | None) -> str:
    if not city:
        return ""
    return CITY_IATA.get(city.strip(), city.strip().upper())


def should_use_sample_fallback(client_id: str | None, client_secret: str | None) -> bool:
    return not (client_id and client_id.strip() and client_secret and client_secret.strip())


def normalize_flight_offers(response: dict, from_city: str, to_city: str) -> list[dict]:
    rows = []
    for offer in response.get("data") or []:
        segments = [
            segment
            for itinerary in offer.get("itineraries") or []
            for segment in itinerary.get("segments") or []
        ]
        if not segments:
            continue

        first = segments[0]
        last = segments[-1]
        carrier = first.get("carrierCode") or (offer.get("validatingAirlineCodes") or [""])[0]
        number = first.get("number", "")
        departure = first.get("departure") or {}
        arrival = last.get("arrival") or {}
        price = _rounded_price((offer.get("price") or {}).get("total"))
        flight = {
            "flight_no": f"{carrier}{number}",
            "airline_name": AIRLINE_NAMES.get(carrier, carrier or "未知航司"),
            "from_city": from_city,
            "to_city": to_city,
            "from_airport": AIRPORT_NAMES.get(departure.get("iataCode"), departure.get("iataCode", "")),
            "to_airport": AIRPORT_NAMES.get(arrival.get("iataCode"), arrival.get("iataCode", "")),
            "depart_time": departure.get("at", ""),
            "arrive_time": arrival.get("at", ""),
            "price": price,
            "seats_left": int(offer.get("numberOfBookableSeats") or 0),
            "data_source": "amadeus",
        }
        if _is_complete(flight):
            rows.append(flight)
    return rows


def fetch_flight_offers(
    from_city: str,
    to_city: str,
    date: str,
    adults: int,
    max_results: int,
    client_id: str | None = None,
    client_secret: str | None = None,
    base_url: str | None = None,
) -> list[dict]:
    client_id = client_id or os.getenv("AMADEUS_CLIENT_ID")
    client_secret = client_secret or os.getenv("AMADEUS_CLIENT_SECRET")
    base_url = (base_url or os.getenv("AMADEUS_BASE_URL") or "https://test.api.amadeus.com").rstrip("/")
    if should_use_sample_fallback(client_id, client_secret):
        return []

    token = _fetch_access_token(base_url, client_id, client_secret)
    response = requests.get(
        f"{base_url}/v2/shopping/flight-offers",
        headers={"Authorization": f"Bearer {token}"},
        params={
            "originLocationCode": city_to_iata(from_city),
            "destinationLocationCode": city_to_iata(to_city),
            "departureDate": date,
            "adults": adults,
            "max": max_results,
            "currencyCode": "CNY",
        },
        timeout=20,
    )
    response.raise_for_status()
    return normalize_flight_offers(response.json(), from_city=from_city, to_city=to_city)


def _fetch_access_token(base_url: str, client_id: str, client_secret: str) -> str:
    response = requests.post(
        f"{base_url}/v1/security/oauth2/token",
        data={
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
        },
        timeout=20,
    )
    response.raise_for_status()
    return response.json()["access_token"]


def _rounded_price(value: str | None) -> int:
    if not value:
        return 0
    return int(Decimal(str(value)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))


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
