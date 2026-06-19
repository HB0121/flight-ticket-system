package com.example.flight.crawl.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataSourceStatusService {

    private final String crawlerCommand;
    private final String aerodataboxEnvKey;
    private final String aerodataboxConfigKey;

    public DataSourceStatusService(@Value("${app.crawler.command:}") String crawlerCommand,
                                   @Value("${AERODATABOX_KEY:}") String aerodataboxEnvKey,
                                   @Value("${aerodatabox.key:}") String aerodataboxConfigKey) {
        this.crawlerCommand = crawlerCommand;
        this.aerodataboxEnvKey = aerodataboxEnvKey;
        this.aerodataboxConfigKey = aerodataboxConfigKey;
    }

    public List<DataSourceStatus> listStatuses() {
        boolean remoteConfigured = hasText(crawlerCommand) && hasText(effectiveAerodataboxKey());

        return List.of(
                new DataSourceStatus("aerodatabox", "AeroDataBox", remoteConfigured, "remote", configurationDetail(remoteConfigured))
        );
    }

    public boolean isConfigured(String source) {
        String normalized = normalizeSource(source);
        return listStatuses().stream()
                .filter(status -> status.code().equalsIgnoreCase(normalized))
                .findFirst()
                .map(DataSourceStatus::configured)
                .orElse(false);
    }

    String effectiveAerodataboxKey() {
        return hasText(aerodataboxEnvKey) ? aerodataboxEnvKey : aerodataboxConfigKey;
    }

    private String normalizeSource(String source) {
        if (source == null) {
            return "";
        }
        return "amadeus".equalsIgnoreCase(source) ? "aerodatabox" : source.trim().toLowerCase();
    }

    private String configurationDetail(boolean remoteConfigured) {
        return remoteConfigured
                ? "Crawler command and AeroDataBox RapidAPI key are configured"
                : "AERODATABOX_KEY or aerodatabox.key is missing, or crawler command is blank";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
