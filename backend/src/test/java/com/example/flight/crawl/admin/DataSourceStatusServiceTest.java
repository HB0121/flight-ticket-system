package com.example.flight.crawl.admin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceStatusServiceTest {

    @Test
    void listsOnlyPhaseOneSafeSources() {
        DataSourceStatusService service = new DataSourceStatusService("docker compose run crawler");

        assertThat(service.listStatuses())
                .extracting(DataSourceStatusService.DataSourceStatus::code)
                .containsExactly("sample", "amadeus");
    }

    @Test
    void marksRemoteSourcesUnavailableWhenCrawlerCommandIsBlank() {
        DataSourceStatusService service = new DataSourceStatusService("   ");

        assertThat(service.listStatuses())
                .extracting(DataSourceStatusService.DataSourceStatus::configured)
                .containsExactly(true, false);
    }
}
