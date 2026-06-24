package com.example.flight.crawl;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrawlServiceTest {

    @Test
    void drainsProcessOutputWhileWaitingForCrawlerToExit() {
        CrawlRepository crawlRepository = mock(CrawlRepository.class);
        CrawlJob success = new CrawlJob(
                1L,
                "SUCCESS",
                LocalDateTime.now(),
                LocalDateTime.now(),
                1,
                0,
                null,
                "aerodatabox",
                "source=aerodatabox, airportCode=CKG, date=2026-06-18"
        );
        when(crawlRepository.findLatest()).thenReturn(Optional.of(success));

        CrawlService service = new CrawlService(crawlRepository, outputFlooderCommand(), 2, "", "");

        CrawlJob result = service.runCrawler(new CrawlRequest("ctrip_live", "Shanghai", "Beijing", null, null, null, null));

        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(crawlRepository, never()).insertFailure(anyString(), anyString(), anyString());
    }

    @Test
    void buildsCrawlerCommandAsArgumentListWithoutShellWrapper() {
        CrawlService service = new CrawlService(
                mock(CrawlRepository.class),
                "docker compose -f ../infra/docker-compose.yml run --rm crawler",
                2,
                "",
                ""
        );

        assertThat(service.buildCommandArguments(new CrawlRequest("aerodatabox", null, null, LocalDate.parse("2026-06-18"), null, null, "CKG")))
                .containsExactly(
                        "docker", "compose", "-f", "../infra/docker-compose.yml", "run", "--rm", "crawler",
                        "scrapy", "crawl", "aerodatabox_flights",
                        "-a", "source=aerodatabox",
                        "-a", "airport_code=CKG",
                        "-a", "date=2026-06-18"
                );
    }

    @Test
    void injectsConfiguredAerodataboxKeyIntoCrawlerEnvironmentWhenEnvVarIsMissing() {
        CrawlService service = new CrawlService(
                mock(CrawlRepository.class),
                "docker compose -f ../infra/docker-compose.yml run --rm crawler",
                2,
                "",
                "config-key"
        );
        var environment = new HashMap<String, String>();

        service.applyProcessEnvironment(environment);

        assertThat(environment).containsEntry("AERODATABOX_KEY", "config-key");
    }

    @Test
    void keepsExistingEnvironmentAerodataboxKeyInsteadOfOverwritingIt() {
        CrawlService service = new CrawlService(
                mock(CrawlRepository.class),
                "docker compose -f ../infra/docker-compose.yml run --rm crawler",
                2,
                "env-key",
                "config-key"
        );
        var environment = new HashMap<String, String>();
        environment.put("AERODATABOX_KEY", "already-present");

        service.applyProcessEnvironment(environment);

        assertThat(environment).containsEntry("AERODATABOX_KEY", "already-present");
    }

    private String outputFlooderCommand() {
        return "java -cp target/test-classes " + OutputFlooder.class.getName();
    }
}
