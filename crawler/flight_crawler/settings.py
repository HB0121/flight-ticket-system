BOT_NAME = "flight_crawler"

SPIDER_MODULES = ["flight_crawler.spiders"]
NEWSPIDER_MODULE = "flight_crawler.spiders"

ROBOTSTXT_OBEY = False
LOG_LEVEL = "INFO"

ITEM_PIPELINES = {
    "flight_crawler.pipelines.MysqlPipeline": 300,
}

REQUEST_FINGERPRINTER_IMPLEMENTATION = "2.7"
TWISTED_REACTOR = "twisted.internet.asyncioreactor.AsyncioSelectorReactor"
FEED_EXPORT_ENCODING = "utf-8"

