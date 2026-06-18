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
        ignoreFailure("alter table crawl_job add column request_params varchar(500) null");
        ignoreFailure(
                "create table if not exists flight_price_snapshot (" +
                " id bigint primary key auto_increment," +
                " flight_id bigint not null," +
                " flight_no varchar(20) not null," +
                " from_city varchar(32) not null," +
                " to_city varchar(32) not null," +
                " depart_time datetime not null," +
                " price decimal(10, 2) not null," +
                " seats_left int not null default 0," +
                " data_source varchar(32) not null," +
                " observed_at datetime not null" +
                " )");
        ignoreFailure("create index idx_snapshot_flight on flight_price_snapshot (flight_id, observed_at)");
        ignoreFailure("create index idx_snapshot_route on flight_price_snapshot (from_city, to_city, depart_time)");
        ignoreFailure("alter table crawl_job add column rejected_count int not null default 0");
        ignoreFailure(
                "create table if not exists flight_validation_failure (" +
                " id bigint primary key auto_increment," +
                " crawl_job_id bigint," +
                " flight_no varchar(20)," +
                " from_city varchar(32)," +
                " to_city varchar(32)," +
                " depart_time datetime," +
                " price decimal(10,2)," +
                " seats_left int," +
                " failure_reason varchar(500) not null," +
                " rejected_at datetime not null" +
                " )");
        ignoreFailure(
                "create table if not exists price_context (" +
                " id bigint primary key auto_increment," +
                " from_city varchar(32) not null," +
                " to_city varchar(32) not null," +
                " depart_date date," +
                " context_text text not null," +
                " context_type varchar(32) not null default 'PRICE_TREND'," +
                " created_at datetime not null" +
                " )");
        ignoreFailure(
                "create table if not exists conversation_session (" +
                " id varchar(36) primary key," +
                " title varchar(128)," +
                " created_at datetime not null," +
                " updated_at datetime not null" +
                " )");
        ignoreFailure(
                "create table if not exists conversation_message (" +
                " id bigint primary key auto_increment," +
                " session_id varchar(36) not null," +
                " role varchar(16) not null," +
                " content text not null," +
                " created_at datetime not null" +
                " )");
        ignoreFailure(
                "create table if not exists app_user (" +
                " id bigint primary key auto_increment," +
                " username varchar(32) unique not null," +
                " password varchar(128) not null," +
                " nickname varchar(32)," +
                " created_at datetime not null" +
                " )");
        ignoreFailure(
                "create table if not exists user_token (" +
                " id bigint primary key auto_increment," +
                " user_id bigint not null," +
                " token varchar(64) unique not null," +
                " created_at datetime not null," +
                " expires_at datetime not null" +
                " )");
        ignoreFailure(
                "create table if not exists favorite (" +
                " id bigint primary key auto_increment," +
                " user_id bigint not null," +
                " flight_id bigint not null," +
                " created_at datetime not null," +
                " constraint uk_favorite_user_flight unique (user_id, flight_id)" +
                " )");
        ignoreFailure("alter table favorite add constraint fk_favorite_user foreign key (user_id) references app_user(id)");
        ignoreFailure("alter table favorite add constraint fk_favorite_flight foreign key (flight_id) references flight(id)");
        ignoreFailure("create index idx_favorite_user_created on favorite (user_id, created_at)");
        ignoreFailure(
                "create table if not exists search_history (" +
                " id bigint primary key auto_increment," +
                " user_id bigint not null," +
                " from_city varchar(32)," +
                " to_city varchar(32)," +
                " travel_date date," +
                " data_source varchar(32)," +
                " created_at datetime not null" +
                " )");
        ignoreFailure("create index idx_search_history_user_created on search_history (user_id, created_at)");
    }

    private void ignoreFailure(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Existing v2 schemas already have these columns/indexes.
        }
    }
}
