package com.example.flight.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSecondVersionSchema() {
        ignoreFailure("alter table crawl_job add column source varchar(32) null");
        ignoreFailure("alter table crawl_job add column actual_source varchar(32) null");
        ignoreFailure("alter table crawl_job add column fallback_reason varchar(500) null");
        ignoreFailure("alter table crawl_job add column request_params varchar(500) null");
        jdbcTemplate.execute("""
                create table if not exists flight_price_snapshot (
                    id bigint primary key auto_increment,
                    flight_id bigint not null,
                    flight_no varchar(20) not null,
                    from_city varchar(32) not null,
                    to_city varchar(32) not null,
                    depart_time datetime not null,
                    price decimal(10, 2) not null,
                    seats_left int not null default 0,
                    data_source varchar(32) not null,
                    observed_at datetime not null
                )
                """);
        ignoreFailure("create index idx_snapshot_flight on flight_price_snapshot (flight_id, observed_at)");
        ignoreFailure("create index idx_snapshot_route on flight_price_snapshot (from_city, to_city, depart_time)");
    }

    private void ignoreFailure(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Existing v2 schemas already have these columns/indexes.
        }
    }
}
