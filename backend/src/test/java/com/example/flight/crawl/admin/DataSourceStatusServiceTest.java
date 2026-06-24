package com.example.flight.crawl.admin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceStatusServiceTest {

    @Test
    void listsOnlyAerodataboxAsConfiguredRemoteSource() {
        DataSourceStatusService service = new DataSourceStatusService("docker compose run crawler", "env-key", "config-key");

        assertThat(service.listStatuses())
                .extracting(DataSourceStatusService.DataSourceStatus::code)
                .containsExactly("aerodatabox");
    }

    @Test
    void marksRemoteSourceUnavailableWhenCrawlerCommandIsBlank() {
        DataSourceStatusService service = new DataSourceStatusService("   ", "env-key", "config-key");

        assertThat(service.listStatuses())
                .extracting(DataSourceStatusService.DataSourceStatus::configured)
                .containsExactly(false);
    }

    @Test
    void treatsLegacyAmadeusSourceAsAerodataboxForConfigurationChecks() {
        DataSourceStatusService service = new DataSourceStatusService("docker compose run crawler", "env-key", "config-key");

        assertThat(service.isConfigured("amadeus")).isTrue();
        assertThat(service.isConfigured("aerodatabox")).isTrue();
    }

    @Test
    void fallsBackToConfiguredKeyWhenEnvironmentKeyIsBlank() {
        DataSourceStatusService service = new DataSourceStatusService("docker compose run crawler", "   ", "config-key");

        assertThat(service.effectiveAerodataboxKey()).isEqualTo("config-key");
        assertThat(service.isConfigured("aerodatabox")).isTrue();
    }

    @Test
    void prefersEnvironmentKeyOverConfiguredKey() {
        DataSourceStatusService service = new DataSourceStatusService("docker compose run crawler", "env-key", "config-key");

        assertThat(service.effectiveAerodataboxKey()).isEqualTo("env-key");
    }
}
