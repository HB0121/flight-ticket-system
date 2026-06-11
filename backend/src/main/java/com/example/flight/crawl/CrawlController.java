package com.example.flight.crawl;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {
    private final CrawlService crawlService;
    private final CrawlRepository crawlRepository;

    public CrawlController(CrawlService crawlService, CrawlRepository crawlRepository) {
        this.crawlService = crawlService;
        this.crawlRepository = crawlRepository;
    }

    @PostMapping("/run")
    public CrawlJob run() {
        return crawlService.runSampleCrawler();
    }

    @GetMapping("/latest")
    public CrawlJob latest() {
        return crawlRepository.findLatest()
                .orElseGet(() -> new CrawlJob(null, "EMPTY", null, null, 0, 0, "暂无采集记录"));
    }
}

