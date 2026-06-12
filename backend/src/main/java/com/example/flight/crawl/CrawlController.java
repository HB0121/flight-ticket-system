package com.example.flight.crawl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 爬虫管理 REST 控制器，暴露 /api/crawl 路径下的爬虫触发和查询端点。
 *
 * 两个核心端点：
 *   - POST /api/crawl/run   ：触发爬虫执行（同步等待完成后返回结果）
 *   - GET  /api/crawl/latest：查询最近一次爬虫任务状态
 *
 * 安全说明：POST /run 端点已通过 WebConfig 配置 LoginInterceptor 保护，
 * 需要登录后才能调用，防止未授权用户频繁触发爬虫。
 */
@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private static final Logger log = LoggerFactory.getLogger(CrawlController.class);
    private final CrawlService crawlService;
    private final CrawlRepository crawlRepository;

    /** 构造器注入：同时依赖 Service 和 Repository（Service 执行，Repository 查询） */
    public CrawlController(CrawlService crawlService, CrawlRepository crawlRepository) {
        this.crawlService = crawlService;
        this.crawlRepository = crawlRepository;
    }

    /**
     * 触发爬虫执行。
     * 接收可选的请求体（CrawlRequest JSON），未提供时使用默认参数。
     * 执行过程为同步阻塞：API 调用会等待爬虫完成（或超时）后才返回响应。
     *
     * @param request 可选的爬虫请求参数（JSON body），null 时使用默认全栈样本爬虫
     * @return 最新 CrawlJob 记录，包含执行状态和统计数据
     */
    @PostMapping("/run")
    public CrawlJob run(@RequestBody(required = false) CrawlRequest request) {
        log.info("接收到爬虫触发请求: source={}", request != null ? request.normalizedSource() : "default");
        // 若 request 为 null，创建一个全默认的 CrawlRequest（source=null 时默认降级为 sample）
        return crawlService.runCrawler(request == null ? new CrawlRequest(null, null, null, null, null, null) : request);
    }

    /**
     * 查询最近一次爬虫执行状态。
     * 用于前端"采集管理"面板展示上次采集的时间、结果和统计数据。
     * 若数据库无任何记录，返回一个占位 EMPTY 状态对象。
     *
     * @return 最新 CrawlJob 记录，或 EMPTY 占位对象
     */
    @GetMapping("/latest")
    public CrawlJob latest() {
        return crawlRepository.findLatest()
                .orElseGet(() -> new CrawlJob(null, "EMPTY", null, null, 0, 0, "暂无采集记录", null, null));
    }
}
