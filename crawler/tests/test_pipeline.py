from flight_crawler.pipelines import MysqlPipeline


def test_close_status_is_success_when_items_succeeded_without_failures():
    assert MysqlPipeline.close_status(success_count=4, failed_count=0, finish_reason=None) == "SUCCESS"


def test_close_status_is_failed_when_items_failed():
    assert MysqlPipeline.close_status(success_count=3, failed_count=1, finish_reason="finished") == "FAILED"
