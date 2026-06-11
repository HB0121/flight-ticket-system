from flight_crawler.parser import parse_flights


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

