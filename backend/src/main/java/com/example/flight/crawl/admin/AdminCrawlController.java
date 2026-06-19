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
    private static final Set<String> ALLOWED_SOURCES = Set.of("amadeus", "aerodatabox");

    private final CrawlService crawlService;
    private final CrawlRepository crawlRepository;
    private final DataSourceStatusService dataSourceStatusService;

    public AdminCrawlController(CrawlService crawlService,
                                CrawlRepository crawlRepository,
                                DataSourceStatusService dataSourceStatusService) {
        this.crawlService = crawlService;
        this.crawlRepository = crawlRepository;
        this.dataSourceStatusService = dataSourceStatusService;
    }

    @PostMapping
    public CrawlJob create(@Valid @RequestBody CreateCrawlJobRequest request) {
        if (!ALLOWED_SOURCES.contains(request.source())) {
            throw new IllegalArgumentException("source must be one of: aerodatabox, amadeus");
        }

        String normalizedSource = normalizeSource(request.source());
        if (!dataSourceStatusService.isConfigured(normalizedSource)) {
            throw new IllegalStateException("source is not configured: " + normalizedSource);
        }

        return crawlService.runCrawler(new CrawlRequest(
                normalizedSource,
                request.fromCity(),
                request.toCity(),
                request.date(),
                request.adults(),
                request.maxResults(),
                request.airportCode()
        ));
    }

    @GetMapping
    public List<CrawlJob> list() {
        return crawlRepository.findRecent(DEFAULT_JOB_LIMIT);
    }

    private String normalizeSource(String source) {
        return "amadeus".equalsIgnoreCase(source) ? "aerodatabox" : source.trim().toLowerCase();
    }

    public record CreateCrawlJobRequest(
            @NotBlank String source,
            String fromCity,
            String toCity,
            LocalDate date,
            @Min(1) Integer adults,
            @Min(1) Integer maxResults,
            String airportCode
    ) {
    }
}
