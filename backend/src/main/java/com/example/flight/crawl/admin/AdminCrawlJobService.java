package com.example.flight.crawl.admin;

import com.example.flight.crawl.CrawlJob;
import com.example.flight.crawl.CrawlRepository;
import com.example.flight.crawl.CrawlRequest;
import com.example.flight.crawl.CrawlService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AdminCrawlJobService {

    private static final int DEFAULT_JOB_LIMIT = 20;
    private static final Set<String> ALLOWED_SOURCES = Set.of("amadeus", "aerodatabox");

    private final CrawlService crawlService;
    private final CrawlRepository crawlRepository;
    private final DataSourceStatusService dataSourceStatusService;

    public AdminCrawlJobService(CrawlService crawlService,
                                CrawlRepository crawlRepository,
                                DataSourceStatusService dataSourceStatusService) {
        this.crawlService = crawlService;
        this.crawlRepository = crawlRepository;
        this.dataSourceStatusService = dataSourceStatusService;
    }

    public CrawlJob create(AdminCrawlJobRequest request) {
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

    public List<CrawlJob> list() {
        return crawlRepository.findRecent(DEFAULT_JOB_LIMIT);
    }

    private String normalizeSource(String source) {
        return "amadeus".equalsIgnoreCase(source) ? "aerodatabox" : source.trim().toLowerCase();
    }
}
