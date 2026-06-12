"""
Scrapy 全局配置文件。

本文件定义了 Scrapy 爬虫项目的核心运行参数，包括：
- 爬虫名称和模块路径
- Robots 协议遵守策略
- 日志级别
- Item Pipeline 加载链
- 指纹器和异步反应器选择

所有以全大写命名的变量均为 Scrapy 内置配置项，
其名称和含义请参考 Scrapy 官方文档。
"""

# ---------- 爬虫基本标识 ----------

# 爬虫项目名称，用于日志和统计标识
BOT_NAME = "flight_crawler"

# 爬虫模块搜索路径：startproject 时自动生成
SPIDER_MODULES = ["flight_crawler.spiders"]
NEWSPIDER_MODULE = "flight_crawler.spiders"

# ---------- 爬取策略 ----------

# 不遵守 robots.txt 规则（本地/测试环境，避免被拒绝爬取）
ROBOTSTXT_OBEY = False

# 日志级别：INFO 平衡详细度和可读性（调试时可改为 DEBUG）
LOG_LEVEL = "INFO"

# ---------- Pipeline 加载链 ----------

# Item Pipeline 注册
# 数字 300 表示优先级（0-1000，数字越小越先执行）
# MysqlPipeline 负责将爬取的航班数据写入 MySQL
ITEM_PIPELINES = {
    "flight_crawler.pipelines.MysqlPipeline": 300,
}

# ---------- 底层引擎配置 ----------

# 使用 Scrapy 2.7 版本的请求指纹实现，确保请求去重行为一致
REQUEST_FINGERPRINTER_IMPLEMENTATION = "2.7"

# 使用 asyncio 异步反应器，支持 async def 语法的 Spider
TWISTED_REACTOR = "twisted.internet.asyncioreactor.AsyncioSelectorReactor"

# Feed 导出文件的字符编码
FEED_EXPORT_ENCODING = "utf-8"
