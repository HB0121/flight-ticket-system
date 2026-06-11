package com.example.flight.crawl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record CrawlRequest(
        String source,
        String fromCity,
        String toCity,
        LocalDate date,
        Integer adults,
        Integer maxResults
) {
    public String normalizedSource() {
        return "amadeus".equalsIgnoreCase(source) ? "amadeus" : "sample";
    }

    public List<String> toCrawlerArguments() {
        if (!"amadeus".equals(normalizedSource())) {
            return List.of("scrapy", "crawl", "sample_flights");
        }
        var args = new ArrayList<String>();
        args.add("scrapy");
        args.add("crawl");
        args.add("amadeus_flights");
        args.add("-a");
        args.add("from_city=" + valueOrDefault(fromCity, "上海"));
        args.add("-a");
        args.add("to_city=" + valueOrDefault(toCity, "北京"));
        args.add("-a");
        args.add("date=" + (date == null ? LocalDate.now().plusDays(7) : date));
        args.add("-a");
        args.add("adults=" + (adults == null || adults < 1 ? 1 : adults));
        args.add("-a");
        args.add("max_results=" + (maxResults == null || maxResults < 1 ? 5 : maxResults));
        return args;
    }

    public String toSummary() {
        return "source=" + normalizedSource()
                + ", fromCity=" + valueOrDefault(fromCity, "上海")
                + ", toCity=" + valueOrDefault(toCity, "北京")
                + ", date=" + (date == null ? LocalDate.now().plusDays(7) : date)
                + ", adults=" + (adults == null || adults < 1 ? 1 : adults)
                + ", maxResults=" + (maxResults == null || maxResults < 1 ? 5 : maxResults);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
