package com.example.flight.crawl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record CrawlRequest(
        String source,
        String fromCity,
        String toCity,
        LocalDate date,
        Integer adults,
        Integer maxResults,
        String airportCode
) {
    private static final String PRIMARY_REMOTE_SOURCE = "aerodatabox";
    private static final String LEGACY_REMOTE_SOURCE = "amadeus";
    private static final Map<String, String> LIVE_SOURCE_PROVIDERS = Map.of(
            "ctrip_live", "ctrip",
            "fliggy_live", "fliggy",
            "qunar_live", "qunar"
    );

    public String normalizedSource() {
        String normalized = source == null ? "" : source.trim().toLowerCase();
        if (LEGACY_REMOTE_SOURCE.equals(normalized)) {
            return PRIMARY_REMOTE_SOURCE;
        }
        if (PRIMARY_REMOTE_SOURCE.equals(normalized) || LIVE_SOURCE_PROVIDERS.containsKey(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("unsupported source: " + source);
    }

    public List<String> toCrawlerArguments() {
        String normalized = normalizedSource();
        if (LIVE_SOURCE_PROVIDERS.containsKey(normalized)) {
            var args = routeArguments("live_flights");
            args.add(3, "-a");
            args.add(4, "provider=" + LIVE_SOURCE_PROVIDERS.get(normalized));
            return args;
        }
        if (hasAirportCode()) {
            return airportArguments();
        }
        throw new IllegalArgumentException("airportCode is required for source: " + normalized);
    }

    private ArrayList<String> routeArguments(String spiderName) {
        var args = new ArrayList<String>();
        args.add("scrapy");
        args.add("crawl");
        args.add(spiderName);
        args.add("-a");
        args.add("from_city=" + valueOrDefault(fromCity, "涓婃捣"));
        args.add("-a");
        args.add("to_city=" + valueOrDefault(toCity, "鍖椾含"));
        args.add("-a");
        args.add("date=" + effectiveDate());
        args.add("-a");
        args.add("adults=" + (adults == null || adults < 1 ? 1 : adults));
        args.add("-a");
        args.add("max_results=" + (maxResults == null || maxResults < 1 ? 5 : maxResults));
        return args;
    }

    private ArrayList<String> airportArguments() {
        var args = new ArrayList<String>();
        args.add("scrapy");
        args.add("crawl");
        args.add("aerodatabox_flights");
        args.add("-a");
        args.add("source=" + normalizedSource());
        args.add("-a");
        args.add("airport_code=" + airportCodeValue());
        args.add("-a");
        args.add("date=" + effectiveDate());
        return args;
    }

    public String toSummary() {
        if (hasAirportCode()) {
            return "source=" + normalizedSource()
                    + ", airportCode=" + airportCodeValue()
                    + ", date=" + effectiveDate();
        }
        return "source=" + normalizedSource()
                + ", fromCity=" + valueOrDefault(fromCity, "涓婃捣")
                + ", toCity=" + valueOrDefault(toCity, "鍖椾含")
                + ", date=" + effectiveDate()
                + ", adults=" + (adults == null || adults < 1 ? 1 : adults)
                + ", maxResults=" + (maxResults == null || maxResults < 1 ? 5 : maxResults);
    }

    private LocalDate effectiveDate() {
        return date == null ? LocalDate.now().plusDays(7) : date;
    }

    private boolean hasAirportCode() {
        return airportCode != null && !airportCode.isBlank();
    }

    private String airportCodeValue() {
        if (!hasAirportCode()) {
            throw new IllegalArgumentException("airportCode is required");
        }
        return airportCode.trim().toUpperCase();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
