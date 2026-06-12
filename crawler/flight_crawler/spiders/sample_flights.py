"""
样例航班爬虫（SampleFlightsSpider）。

这个 Spider 读取本地静态 HTML 文件 sample_pages/flights.html，
并使用 parser 模块将其解析为航班数据。它是系统"优雅降级"策略
的基础组件：当 Amadeus API 不可用时，系统回退到此处读取样例数据，
确保在任何环境下都能正常演示。

Scrapy Spider 契约：
- name: "sample_flights" —— 供 `scrapy crawl sample_flights` 命令使用
- start(): 异步启动方法（使用 asyncio reactor）
- start_requests(): 生成初始 Request 列表
- parse(): 回调函数，解析响应并 yield flight dict

数据流：
  sample_pages/flights.html  ──(file:// URI)──►  scrapy.Request
        │                                              │
        │                                   callback=self.parse
        ▼                                              │
  parse_flights(response.text)  ◄──────────────────────┘
        │
        ▼
  yield flight dict  ──►  MysqlPipeline.process_item()
"""

from pathlib import Path

import scrapy

from flight_crawler.parser import parse_flights


class SampleFlightsSpider(scrapy.Spider):
    """样例数据爬虫

    读取本地 sample_pages/flights.html 文件，
    解析其中包含的航班卡片数据并 yield 给 Pipeline。

    与 AmadeusFlightsSpider 的区别：
    - 不调用外部 API，纯本地文件读取
    - data_source 固定为 "sample"
    - 不需要任何凭证或网络连接
    """

    # Scrapy Spider 唯一标识名，通过 scrapy crawl sample_flights 调用
    name = "sample_flights"

    async def start(self):
        """异步启动入口。

        使用 asyncio reactor 时 Scrapy 会调用此方法而非 start_requests()。
        此处将 start_requests() 生成的 Request 逐个异步 yield 出去，
        保持与同步模式的兼容性。
        """
        for request in self.start_requests():
            yield request

    def start_requests(self):
        """生成初始请求。

        使用 Path(__file__) 动态计算 sample_pages/flights.html 的绝对路径，
        然后用 file:// URI 发起 Scrapy Request，确保在任何 CWD 下都能找到文件。

        返回:
            生成器，yield 单个 scrapy.Request 对象
        """
        # 从当前文件向上两级目录找到项目根，再定位 sample_pages/flights.html
        sample_path = Path(__file__).resolve().parents[2] / "sample_pages" / "flights.html"
        # file:// URI 让 Scrapy 能够读取本地文件
        yield scrapy.Request(sample_path.as_uri(), callback=self.parse)

    def parse(self, response):
        """解析 HTML 响应，yield 每条航班数据。

        这是 scrapy.Request 的回调函数。收到本地 HTML 文件内容后，
        调用 parser.parse_flights 将其转为 dict 列表，逐条 yield。
        每条 dict 将被 Scrapy Engine 传递给 Item Pipeline 处理。

        参数:
            response: Scrapy Response 对象，response.text 为完整 HTML 字符串
        """
        # 委托给 parser 模块做实际解析，Spider 只负责请求调度
        for flight in parse_flights(response.text):
            yield flight
