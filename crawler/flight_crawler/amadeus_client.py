"""
Amadeus Flight Offers Search API 客户端。

本模块封装了对 Amadeus Self-Service API 的调用流程，
实现了从中文城市名到 IATA 代码的映射、OAuth2 认证、
航班数据请求 以及 响应格式化为内部 dict 的全过程。

核心流程（fetch_flight_offers）:
  1. 检查凭证 → 若无则返回空列表（触发上游回退至 sample 数据）
  2. OAuth2 Client Credentials 获取 access token
  3. GET /v2/shopping/flight-offers 请求航班数据
  4. normalize_flight_offers 将 API 响应标准化为本地格式

设计约束：
- 所有外部依赖（Amadeus API、网络）都必须"优雅降级"：
  API 不可用时返回空列表，由 Spider 层回退到样例数据，
  确保系统在任何情况下都能正常运行演示。

数据映射：
- 城市名称 → IATA 代码：见 CITY_IATA 字典（中文 → 三字码）
- IATA 机场代码 → 中文名称：见 AIRPORT_NAMES 字典
- IATA 航司代码 → 中文名称：见 AIRLINE_NAMES 字典
"""

import os
from decimal import Decimal, ROUND_HALF_UP

import requests


# ==================== 城市/机场/航司编码映射 ====================

# 中文城市名 → IATA 三字码
# 系统使用中文城市名作为领域语言，仅在 Amadeus API 边界转换为 IATA 码
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

# IATA 机场代码 → 中文机场名称
# 用于将 API 返回的纯英文代码转为用户可读的中文名
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

# IATA 航司代码 → 中文航司名称
# 用于将 API 返回的如 "MU" 转为 "东方航空"
AIRLINE_NAMES = {
    "CA": "中国国航",
    "MU": "东方航空",
    "CZ": "南方航空",
    "HU": "海南航空",
    "ZH": "深圳航空",
    "MF": "厦门航空",
    "3U": "四川航空",
}


# ==================== 公开 API ====================


def city_to_iata(city: str | None) -> str:
    """将中文城市名转换为 IATA 三字码。

    转换规则：
    1. 先在 CITY_IATA 字典中精确查找
    2. 未找到则回退：去除空白后转为大写（可能本身已是 IATA 码）

    参数:
        city: 中文城市名（如 "上海"）或 IATA 码

    返回:
        str: IATA 三字码，输入为空时返回空字符串
    """
    if not city:
        return ""
    return CITY_IATA.get(city.strip(), city.strip().upper())


def should_use_sample_fallback(client_id: str | None, client_secret: str | None) -> bool:
    """判断是否应回退到样例数据。

    当 Amadeus API 凭证（Client ID 和 Client Secret）
    任一为空或仅含空白字符时，返回 True。

    这是"优雅降级"策略的核心判断点：
    没有 API 密钥 → 不尝试调用 API → 直接使用本地样例数据。

    参数:
        client_id: Amadeus API Client ID
        client_secret: Amadeus API Client Secret

    返回:
        bool: True 表示应使用样例数据回退
    """
    return not (client_id and client_id.strip() and client_secret and client_secret.strip())


def normalize_flight_offers(response: dict, from_city: str, to_city: str) -> list[dict]:
    """将 Amadeus Flight Offers API 原始响应标准化为内部格式。

    处理逻辑：
    1. 遍历 response["data"] 中的每个 offer
    2. 提取 segments（航段），取第一个航段的出发信息 + 最后一个航段的到达信息
       这支持直飞和联程航班（联程航班显示全程起降）
    3. 从 carrierCode 和 number 拼出 flight_no
    4. 用 AIRLINE_NAMES / AIRPORT_NAMES 字典将代码转为中文名称
    5. 价格从 API 的 price.total 字段提取并四舍五入到整数

    参数:
        response: Amadeus API 返回的 JSON 字典（已解析）
        from_city: 用户指定的出发城市中文名
        to_city: 用户指定的到达城市中文名

    返回:
        list[dict]: 标准化后的航班字典列表，字段与 FlightItem 对应。
                    不完整或价格为 0 的记录会被过滤掉。
    """
    rows = []
    # Amadeus API 数据在 "data" 数组中
    for offer in response.get("data") or []:
        # 展平所有行程中的所有航段
        segments = [
            segment
            for itinerary in offer.get("itineraries") or []
            for segment in itinerary.get("segments") or []
        ]
        if not segments:
            continue

        # 第一个航段的出发 = 整个行程的出发
        first = segments[0]
        # 最后一个航段的到达 = 整个行程的到达（支持联程）
        last = segments[-1]

        # 航司代码优先取第一个航段的 carrierCode，其次取验证航司列表的第一个
        carrier = first.get("carrierCode") or (offer.get("validatingAirlineCodes") or [""])[0]
        number = first.get("number", "")
        departure = first.get("departure") or {}
        arrival = last.get("arrival") or {}

        # 价格四舍五入到整数元
        price = _rounded_price((offer.get("price") or {}).get("total"))

        flight = {
            # 航班号：航司代码 + 航班数字编号，如 "MU5101"
            "flight_no": f"{carrier}{number}",
            # 中文航司名称，未知时回退到原始代码或 "未知航司"
            "airline_name": AIRLINE_NAMES.get(carrier, carrier or "未知航司"),
            # 城市名沿用用户输入的原始中文名（保留领域语言）
            "from_city": from_city,
            "to_city": to_city,
            # 机场名从 API 返回的 IATA 代码映射为中文名
            "from_airport": AIRPORT_NAMES.get(departure.get("iataCode"), departure.get("iataCode", "")),
            "to_airport": AIRPORT_NAMES.get(arrival.get("iataCode"), arrival.get("iataCode", "")),
            # 时间字段保留 API 原始格式（ISO 8601）
            "depart_time": departure.get("at", ""),
            "arrive_time": arrival.get("at", ""),
            "price": price,
            "seats_left": int(offer.get("numberOfBookableSeats") or 0),
            "data_source": "amadeus",
        }
        # 完整性校验：确保所有必填字段非空且价格有效
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
    """从 Amadeus API 获取航班数据（全流程入口）。

    这是本模块的主入口函数，封装了完整的 Amadeus API 调用链：
    1. 凭证检查（支持参数传入和环境变量两种方式）
    2. OAuth2 令牌获取
    3. Flight Offers Search API 调用
    4. 响应标准化

    参数:
        from_city: 出发城市中文名（如 "上海"）
        to_city: 到达城市中文名（如 "北京"）
        date: 出发日期，ISO 格式 YYYY-MM-DD（如 "2025-06-15"）
        adults: 成人乘客数量
        max_results: 最大返回结果数（Amadeus 限制 ≤ 250）
        client_id: Amadeus API Key（可选，不传则从环境变量读取）
        client_secret: Amadeus API Secret（可选，不传则从环境变量读取）
        base_url: Amadeus API 基础 URL（可选，不传则从环境变量或默认测试环境读取）

    返回:
        list[dict]: 标准化航班列表。凭证缺失时返回空列表 []，
                    调用方应回退到样例数据。
    """
    # 凭证获取优先级：参数 > 环境变量
    client_id = client_id or os.getenv("AMADEUS_CLIENT_ID")
    client_secret = client_secret or os.getenv("AMADEUS_CLIENT_SECRET")
    base_url = (base_url or os.getenv("AMADEUS_BASE_URL") or "https://test.api.amadeus.com").rstrip("/")

    # 凭证缺失 → 优雅降级，返回空列表让上游使用样例数据
    if should_use_sample_fallback(client_id, client_secret):
        return []

    # 步骤 1: OAuth2 Client Credentials 获取 access token
    token = _fetch_access_token(base_url, client_id, client_secret)

    # 步骤 2: 调用 Flight Offers Search API
    response = requests.get(
        f"{base_url}/v2/shopping/flight-offers",
        headers={"Authorization": f"Bearer {token}"},
        params={
            # 中文城市名 → IATA 三字码转换
            "originLocationCode": city_to_iata(from_city),
            "destinationLocationCode": city_to_iata(to_city),
            "departureDate": date,
            "adults": adults,
            "max": max_results,
            "currencyCode": "CNY",  # 人民币计价
        },
        timeout=20,
    )
    # 非 2xx 状态码抛出异常，由调用方捕获并回退
    response.raise_for_status()

    # 步骤 3: 标准化原始 JSON 为内部格式
    return normalize_flight_offers(response.json(), from_city=from_city, to_city=to_city)


# ==================== 内部辅助函数 ====================


def _fetch_access_token(base_url: str, client_id: str, client_secret: str) -> str:
    """通过 OAuth2 Client Credentials 流程获取 Amadeus access token。

    调用 POST /v1/security/oauth2/token 端点，
    使用 client_credentials grant type。

    参数:
        base_url: Amadeus API 基础 URL
        client_id: API Client ID
        client_secret: API Client Secret

    返回:
        str: access token 字符串（Bearer token）

    异常:
        requests.HTTPError: 认证失败时由 raise_for_status() 抛出
    """
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
    """将 API 返回的价格字符串四舍五入为整数。

    Amadeus API 返回的 price.total 是字符串（如 "1280.50"），
    此处使用 Decimal 精确四舍五入到整数，避免浮点精度问题。

    参数:
        value: 价格字符串（可能为 None 或空）

    返回:
        int: 四舍五入后的整数值，输入无效时返回 0
    """
    if not value:
        return 0
    # Decimal 四舍五入到整数位（ROUND_HALF_UP）
    return int(Decimal(str(value)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def _is_complete(flight: dict) -> bool:
    """验证航班字典完整性（与 parser._is_complete 逻辑一致）。

    必填字段 + 价格 > 0 检查，防止不完整或占位数据进入流水线。

    参数:
        flight: 标准化后的航班字典

    返回:
        bool: 完整且价格有效时为 True
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
