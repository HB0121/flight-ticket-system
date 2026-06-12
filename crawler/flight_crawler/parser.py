"""
HTML 航班数据解析模块。

本模块负责将 sample_pages/flights.html 中的静态航班信息
解析为 dict 列表，供 Spider 和 Pipeline 使用。

解析策略：
- 使用 BeautifulSoup 定位 .flight-card CSS 选择器
- 每个 .flight-card 元素包含一个航班的所有信息
- 通过 class 选择器 + data 属性提取各字段
- 只有字段完整（_is_complete）的航班才会被返回

设计模式：
- 解析逻辑与 Spider 解耦：Spider 只负责请求获取 HTML，
  parser 负责 HTML → dict 转换，便于单元测试。
"""

import re
from bs4 import BeautifulSoup


def parse_flights(html: str) -> list[dict]:
    """解析 sample_pages/flights.html 中的航班列表。

    遍历 HTML 中所有 .flight-card 元素，对每个 card 提取
    航班号、航空公司、起降城市/机场、时间、票价、余座等信息。

    参数:
        html: 完整的 HTML 文档字符串

    返回:
        list[dict]: 航班字典列表，每个 dict 包含 FlightItem 所需的全部字段。
                    不完整的记录（缺少必填字段或价格为 0）会被自动丢弃。
    """
    # 使用内置 html.parser 解析，无外部依赖
    soup = BeautifulSoup(html, "html.parser")
    flights = []

    # 遍历所有航班卡片
    for card in soup.select(".flight-card"):
        flight = {
            # 航班号取自 data-flight-no 属性（自定义属性）
            "flight_no": card.get("data-flight-no", "").strip(),
            # 航空公司名称取自 .airline 元素的文本内容
            "airline_name": _text(card, ".airline"),
            # 城市和机场名称通过 CSS class 定位
            "from_city": _text(card, ".from-city"),
            "to_city": _text(card, ".to-city"),
            "from_airport": _text(card, ".from-airport"),
            "to_airport": _text(card, ".to-airport"),
            # 时间信息存储在 datetime 属性中（ISO 8601 格式）
            "depart_time": _attr(card, ".depart-time", "datetime"),
            "arrive_time": _attr(card, ".arrive-time", "datetime"),
            # 价格和余座从文本中提取数字部分
            "price": _number(_text(card, ".price")),
            "seats_left": _number(_text(card, ".seats-left")),
            # 标识数据来源为样例数据
            "data_source": "sample",
        }
        # 只保留字段完整的航班记录，过滤掉缺字段的卡片
        if _is_complete(flight):
            flights.append(flight)

    return flights


# ==================== 内部辅助函数 ====================


def _text(card, selector: str) -> str:
    """提取元素内文本。

    参数:
        card: BeautifulSoup Tag 对象（.flight-card 元素）
        selector: CSS 选择器

    返回:
        str: 元素的文本内容（已 strip），元素不存在时返回空字符串
    """
    node = card.select_one(selector)
    return node.get_text(strip=True) if node else ""


def _attr(card, selector: str, attr: str) -> str:
    """提取元素的 HTML 属性值。

    参数:
        card: BeautifulSoup Tag 对象
        selector: CSS 选择器
        attr: 属性名，如 "datetime"

    返回:
        str: 属性值（已 strip），元素无效时返回空字符串
    """
    node = card.select_one(selector)
    return node.get(attr, "").strip() if node else ""


def _number(value: str) -> int:
    """从字符串中提取第一个数字片段并转为整数。

    处理逻辑：
    1. 先去掉千分位逗号（如 "1,280" → "1280"）
    2. 用正则匹配第一个连续数字序列
    3. 转为 int 返回

    参数:
        value: 含数字的字符串，如 "¥1,280元"、">10"

    返回:
        int: 提取的整数值，无数字时返回 0
    """
    # 去除千分位逗号，防止正则匹配时被逗号截断
    match = re.search(r"\d+", value.replace(",", ""))
    return int(match.group(0)) if match else 0


def _is_complete(flight: dict) -> bool:
    """校验航班 dict 是否包含所有必填字段且价格有效。

    必填字段包括：
    - flight_no（航班号）
    - airline_name（航空公司）
    - from_city / to_city（出发/到达城市）
    - from_airport / to_airport（出发/到达机场）
    - depart_time / arrive_time（出发/到达时间）

    同时要求 price > 0（爬取失败或占位数据价格为 0 的应丢弃）。

    参数:
        flight: 航班字典

    返回:
        bool: 所有必填字段非空且价格大于 0 时为 True
    """
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
