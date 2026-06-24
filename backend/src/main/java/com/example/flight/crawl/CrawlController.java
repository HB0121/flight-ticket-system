package com.example.flight.crawl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private static final Logger log = LoggerFactory.getLogger(CrawlController.class);
    private final CrawlService crawlService;
    private final CrawlRepository crawlRepository;

    public CrawlController(CrawlService crawlService, CrawlRepository crawlRepository) {
        this.crawlService = crawlService;
        this.crawlRepository = crawlRepository;
    }

    @PostMapping("/run")
    public CrawlJob run(@RequestBody(required = false) CrawlRequest request) {
        log.info("Receive crawl trigger request: source={}", request != null ? request.normalizedSource() : "default");
        return crawlService.runCrawler(request == null ? new CrawlRequest(null, null, null, null, null, null, null) : request);
    }

    @GetMapping("/latest")
    public CrawlJob latest() {
        return crawlRepository.findLatest()
                .orElseGet(() -> new CrawlJob(null, "EMPTY", null, null, 0, 0, "No crawl history yet", null, null));
    }
}
