"""
Amadeus 实时航班爬虫（AmadeusFlightsSpider）。

本 Spider 是系统的核心数据采集器，负责从 Amadeus API 获取实时航班数据。
它实现了"优雅降级"的三层回退策略：

  层次 1: Amadeus API 实时数据（需凭证）
      ↓ 凭证缺失 或 API 调用异常
  层次 2: 本地样例数据 sample_pages/flights.html（兜底）
      ↓
  层次 3: 空结果（极端情况下不崩溃）

Scrapy Spider 契约：
- name: "amadeus_flights" —— 供 `scrapy crawl amadeus_flights` 命令使用
- __init__(): 接收命令行参数（from_city, to_city, date, adults, max_results）
- start(): 异步入口，收集航班数据并 yield
- _collect_flights(): 核心采集逻辑（try Amadeus → catch fallback）
- _sample_flights(): 回退到本地样例数据

使用示例：
  scrapy crawl amadeus_flights -a from_city=上海 -a to_city=北京 -a date=2025-06-15

设计模式：Template Method
  - start() 定义骨架（收集 → yield），_collect_flights() 由子类可覆盖
  - 当前只有一个 Spider 实现，但骨架支持未来扩展（如多个数据源组合）
"""

from datetime import date, timedelta
from pathlib import Path

import scrapy

from flight_crawler.amadeus_client import fetch_flight_offers
from flight_crawler.parser import parse_flights


class AmadeusFlightsSpider(scrapy.Spider):
    """Amadeus 实时航班爬虫

    通过 Amadeus Flight Offers Search API 采集实时航班数据，
    API 不可用时自动回退到本地样例数据，保证系统稳定运行。
    """

    # Scrapy Spider 唯一标识名
    name = "amadeus_flights"

    def __init__(
        self,
        from_city="上海",
        to_city="北京",
        date=None,
        adults="1",
        max_results="50",
        *args,
        **kwargs,
    ):
        """初始化 Spider 参数。

        所有参数均可通过 Scrapy -a 命令行参数传入，同时提供合理默认值。

        参数:
            from_city: 出发城市中文名，默认 "上海"
            to_city: 到达城市中文名，默认 "北京"
            date: 出发日期 YYYY-MM-DD，默认一周后
            adults: 成人数量（字符串，需转为 int），默认 "1"
            max_results: 最大结果数（字符串，需转为 int），默认 "5"
        """
        super().__init__(*args, **kwargs)
        self.from_city = from_city
        self.to_city = to_city
        # 日期默认为一周后，给用户留出合理的预订窗口
        self.date = date or (date_today() + timedelta(days=7)).isoformat()
        self.adults = int(adults)
        self.max_results = int(max_results)

        # 以下属性供 MysqlPipeline 进行审计记录
        # source: 数据来源标识
        self.source = "amadeus"
        # request_params: 请求参数摘要，写入 crawl_job 表的 request_params 列
        self.request_params = (
            f"source=amadeus, fromCity={self.from_city}, toCity={self.to_city}, "
            f"date={self.date}, adults={self.adults}, maxResults={self.max_results}"
        )

    async def start(self):
        """异步启动入口。

        收集航班数据（可能是 Amadeus 实时数据或本地样例数据），
        并逐条异步 yield 给 Scrapy Engine，Engine 再传递给 Pipeline。
        """
        for flight in self._collect_flights():
            yield flight

    def _collect_flights(self):
        """核心数据采集：尝试 Amadeus API，失败则回退到样例数据。

        执行流程（三层回退）：
        1. 尝试调用 fetch_flight_offers 获取实时数据
           - 凭证缺失时返回空列表 → 进入步骤 2
           - API 异常（网络/认证等）→ 记录警告日志，进入步骤 2
        2. 回退调用 _sample_flights() 读取本地样例 HTML
        3. 样例文件损坏时 parse_flights 返回空列表（不崩溃）

        Returns:
            list[dict]: 航班字典列表
        """
        try:
            # 层次 1: 尝试 Amadeus API 实时数据
            flights = fetch_flight_offers(
                from_city=self.from_city,
                to_city=self.to_city,
                date=self.date,
                adults=self.adults,
                max_results=self.max_results,
            )
            if flights:
                # API 返回了有效数据，直接返回
                return flights
            # API 返回空列表（凭证缺失或结果为空）→ 回退
            self.logger.warning("Amadeus returned no rows or credentials are missing; using sample fallback.")
        except Exception as exc:
            # API 调用异常（网络错误、超时、认证失败等）→ 回退
            self.logger.warning("Amadeus collection failed; using sample fallback: %s", exc)

        # 层次 2: 回退到本地样例数据
        return self._sample_flights()

    def _sample_flights(self):
        """回退方案：读取本地样例航班 HTML 文件。

        使用 parser.parse_flights 解析样例数据。
        路径计算方式与 SampleFlightsSpider.start_requests 一致，
        确保各种工作目录下都能找到文件。

        Returns:
            list[dict]: 从样例 HTML 解析出的航班字典列表
        """
        # 从当前文件向上两级找到项目根目录
        sample_path = Path(__file__).resolve().parents[2] / "sample_pages" / "flights.html"
        # 直接读取文件内容并委托给 parser 解析
        return parse_flights(sample_path.read_text(encoding="utf-8"))


def date_today():
    """返回当前日期（可被测试 mock 的工厂函数）。

    将 date.today() 封装为独立函数，而非在 __init__ 中直接调用，
    这样单元测试中可以通过 mock 此函数来控制"今天"的日期。

    Returns:
        date: 当前日期对象
    """
    return date.today()
