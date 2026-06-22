package com.example.flight.crawl.admin;

import com.example.flight.crawl.CrawlJob;
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

@RestController
@RequestMapping("/api/admin/crawl-jobs")
public class AdminCrawlController {

    private final AdminCrawlJobService adminCrawlJobService;

    public AdminCrawlController(AdminCrawlJobService adminCrawlJobService) {
        this.adminCrawlJobService = adminCrawlJobService;
    }

    @PostMapping
    public CrawlJob create(@Valid @RequestBody CreateCrawlJobRequest request) {
        return adminCrawlJobService.create(request.toServiceRequest());
    }

    @GetMapping
    public List<CrawlJob> list() {
        return adminCrawlJobService.list();
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
        AdminCrawlJobRequest toServiceRequest() {
            return new AdminCrawlJobRequest(source, fromCity, toCity, date, adults, maxResults, airportCode);
        }
    }
}
