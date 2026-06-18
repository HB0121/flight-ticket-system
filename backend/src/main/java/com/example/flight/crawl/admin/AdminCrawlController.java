package com.example.flight.crawl.admin;

import com.example.flight.crawl.CrawlJob;
import com.example.flight.crawl.CrawlRepository;
import com.example.flight.crawl.CrawlRequest;
import com.example.flight.crawl.CrawlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/crawl-jobs")
public class AdminCrawlController {

    private static final int DEFAULT_JOB_LIMIT = 20;
    private static final Set<String> ALLOWED_SOURCES = Set.of("sample", "amadeus");

    private final CrawlService crawlService;
    private final CrawlRepository crawlRepository;

    public AdminCrawlController(CrawlService crawlService, CrawlRepository crawlRepository) {
        this.crawlService = crawlService;
        this.crawlRepository = crawlRepository;
    }

    @PostMapping
    public CrawlJob create(@Valid @RequestBody CreateCrawlJobRequest request) {
        if (!ALLOWED_SOURCES.contains(request.source())) {
            throw new IllegalArgumentException("source must be one of: sample, amadeus");
        }

        return crawlService.runCrawler(new CrawlRequest(
                request.source(),
                request.fromCity(),
                request.toCity(),
                request.date(),
                request.adults(),
                request.maxResults()
        ));
    }

    @GetMapping
    public List<CrawlJob> list() {
        return crawlRepository.findRecent(DEFAULT_JOB_LIMIT);
    }

    public record CreateCrawlJobRequest(
            @NotBlank String source,
            String fromCity,
            String toCity,
            LocalDate date,
            @Min(1) Integer adults,
            @Min(1) Integer maxResults
    ) {
    }
}
