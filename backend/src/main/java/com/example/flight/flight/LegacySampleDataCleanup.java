package com.example.flight.flight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LegacySampleDataCleanup {

    private static final Logger log = LoggerFactory.getLogger(LegacySampleDataCleanup.class);

    private final JdbcTemplate jdbcTemplate;

    public LegacySampleDataCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void purgeSampleData() {
        int deletedSnapshots = jdbcTemplate.update(
                "delete from flight_price_snapshot where data_source = ?",
                "sample"
        );
        int deletedFlights = jdbcTemplate.update(
                "delete from flight where data_source = ?",
                "sample"
        );
        int deletedJobs = jdbcTemplate.update(
                "delete from crawl_job where source = ? or request_params like ?",
                "sample",
                "source=sample%"
        );

        if (deletedSnapshots + deletedFlights + deletedJobs > 0) {
            log.info(
                    "Removed legacy sample data: {} snapshots, {} flights, {} crawl jobs",
                    deletedSnapshots,
                    deletedFlights,
                    deletedJobs
            );
        }
    }
}
