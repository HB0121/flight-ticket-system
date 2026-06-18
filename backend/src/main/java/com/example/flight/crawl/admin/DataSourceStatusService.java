package com.example.flight.crawl.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataSourceStatusService {

    private final String crawlerCommand;

    public DataSourceStatusService(@Value("${app.crawler.command:}") String crawlerCommand) {
        this.crawlerCommand = crawlerCommand;
    }

    public List<DataSourceStatus> listStatuses() {
        boolean remoteConfigured = crawlerCommand != null && !crawlerCommand.isBlank();

        return List.of(
                new DataSourceStatus("sample", "Sample", true, "fallback", "Built-in fallback spider"),
                new DataSourceStatus("amadeus", "Amadeus", remoteConfigured, "remote", configurationDetail(remoteConfigured))
        );
    }

    private String configurationDetail(boolean remoteConfigured) {
        return remoteConfigured
                ? "Crawler command configured for this source"
                : "Crawler command not configured for this source";
    }

    public record DataSourceStatus(
            String code,
            String label,
            boolean configured,
            String mode,
            String detail
    ) {
    }
}
