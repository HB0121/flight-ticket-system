import re

from bs4 import BeautifulSoup


CAPTCHA_MARKERS = (
    "captcha",
    "验证码",
    "安全验证",
    "风险控制",
    "risk control",
)
LOGIN_MARKERS = (
    "请登录",
    "登录后",
    "login",
    "sign in",
)


def parse_live_flights(html, data_source, from_city, to_city, date, max_results=5):
    soup = BeautifulSoup(html or "", "html.parser")
    page_text = soup.get_text(" ", strip=True)
    block_reason = detect_block_reason(page_text, soup)
    if block_reason:
        return [], block_reason

    flights = []
    for card in soup.select(".live-flight-card, [data-flight-card]"):
        flight = parse_card(card, data_source, from_city, to_city, date)
        if flight:
            flights.append(flight)
        if len(flights) >= int(max_results):
            break

    if not flights:
        return [], "NO_FLIGHT_ROWS_PARSED"
    return flights, None


def detect_block_reason(page_text, soup):
    lowered = page_text.lower()
    if any(marker.lower() in lowered for marker in CAPTCHA_MARKERS):
        return "CAPTCHA_OR_RISK_CONTROL"
    if any(marker.lower() in lowered for marker in LOGIN_MARKERS):
        return "LOGIN_REQUIRED"
    if soup.select("script") and len(page_text) < 20:
        return "JS_RENDERED_EMPTY_PAGE"
    return None


def parse_card(card, data_source, fallback_from_city, fallback_to_city, date):
    flight_no = text_for(card, ".flight-no", "data-flight-no")
    price = number_for(text_for(card, ".price", "data-price"))
    if not flight_no or price is None:
        return None

    depart_time = text_for(card, ".depart-time", "data-depart-time") or f"{date}T00:00:00"
    arrive_time = text_for(card, ".arrive-time", "data-arrive-time") or depart_time
    seats_left = number_for(text_for(card, ".seats-left", "data-seats-left"))

    return {
        "flight_no": flight_no,
        "airline_name": text_for(card, ".airline-name", "data-airline-name") or flight_no[:2],
        "from_city": text_for(card, ".from-city", "data-from-city") or fallback_from_city,
        "to_city": text_for(card, ".to-city", "data-to-city") or fallback_to_city,
        "from_airport": text_for(card, ".from-airport", "data-from-airport") or fallback_from_city,
        "to_airport": text_for(card, ".to-airport", "data-to-airport") or fallback_to_city,
        "depart_time": normalize_time(depart_time, date),
        "arrive_time": normalize_time(arrive_time, date),
        "price": price,
        "seats_left": seats_left if seats_left is not None else 0,
        "data_source": data_source,
    }


def text_for(card, selector, attribute):
    if card.has_attr(attribute):
        return card.get(attribute, "").strip()
    node = card.select_one(selector)
    if not node:
        return ""
    return node.get_text(" ", strip=True)


def number_for(value):
    if value is None:
        return None
    match = re.search(r"\d+", str(value).replace(",", ""))
    if not match:
        return None
    return int(match.group(0))


def normalize_time(value, date):
    value = str(value).strip()
    if "T" in value:
        return value
    if re.match(r"^\d{2}:\d{2}$", value):
        return f"{date}T{value}:00"
    return value
