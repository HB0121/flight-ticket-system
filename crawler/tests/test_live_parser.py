from flight_crawler.live_parser import parse_live_flights


def test_live_parser_extracts_minimal_flight_card():
    html = """
    <html><body>
      <article class="live-flight-card">
        <span class="flight-no">MU5101</span>
        <span class="airline-name">东方航空</span>
        <span class="from-city">上海</span>
        <span class="to-city">北京</span>
        <span class="from-airport">虹桥T2</span>
        <span class="to-airport">首都T2</span>
        <time class="depart-time">2026-06-19T08:00:00</time>
        <time class="arrive-time">2026-06-19T10:20:00</time>
        <span class="price">980</span>
        <span class="seats-left">4</span>
      </article>
    </body></html>
    """

    flights, error = parse_live_flights(
        html,
        data_source="ctrip_live",
        from_city="上海",
        to_city="北京",
        date="2026-06-19",
        max_results=5,
    )

    assert error is None
    assert flights == [
        {
            "flight_no": "MU5101",
            "airline_name": "东方航空",
            "from_city": "上海",
            "to_city": "北京",
            "from_airport": "虹桥T2",
            "to_airport": "首都T2",
            "depart_time": "2026-06-19T08:00:00",
            "arrive_time": "2026-06-19T10:20:00",
            "price": 980,
            "seats_left": 4,
            "data_source": "ctrip_live",
        }
    ]


def test_live_parser_detects_login_required_page():
    flights, error = parse_live_flights(
        "<html><body>请登录后继续 login</body></html>",
        data_source="ctrip_live",
        from_city="上海",
        to_city="北京",
        date="2026-06-19",
        max_results=5,
    )

    assert flights == []
    assert error == "LOGIN_REQUIRED"


def test_live_parser_detects_captcha_or_risk_control_page():
    flights, error = parse_live_flights(
        "<html><body>安全验证 captcha</body></html>",
        data_source="ctrip_live",
        from_city="上海",
        to_city="北京",
        date="2026-06-19",
        max_results=5,
    )

    assert flights == []
    assert error == "CAPTCHA_OR_RISK_CONTROL"


def test_live_parser_detects_js_rendered_empty_page():
    flights, error = parse_live_flights(
        "<html><head><script src=\"app.js\"></script></head><body><div id=\"app\"></div></body></html>",
        data_source="ctrip_live",
        from_city="上海",
        to_city="北京",
        date="2026-06-19",
        max_results=5,
    )

    assert flights == []
    assert error == "JS_RENDERED_EMPTY_PAGE"


def test_live_parser_reports_no_flight_rows_when_static_html_has_no_cards():
    flights, error = parse_live_flights(
        "<html><body><main>公开搜索页，但没有可识别航班行</main></body></html>",
        data_source="ctrip_live",
        from_city="上海",
        to_city="北京",
        date="2026-06-19",
        max_results=5,
    )

    assert flights == []
    assert error == "NO_FLIGHT_ROWS_PARSED"
