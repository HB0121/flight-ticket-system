create table if not exists flight (
    id bigint primary key auto_increment,
    flight_no varchar(20) not null,
    airline_name varchar(64) not null,
    from_city varchar(32) not null,
    to_city varchar(32) not null,
    from_airport varchar(64) not null,
    to_airport varchar(64) not null,
    depart_time datetime not null,
    arrive_time datetime not null,
    price decimal(10, 2) not null,
    seats_left int not null default 0,
    data_source varchar(32) not null,
    collected_at datetime not null,
    unique key uk_flight_source (flight_no, depart_time, data_source),
    key idx_route_date (from_city, to_city, depart_time),
    key idx_price (price)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists crawl_job (
    id bigint primary key auto_increment,
    status varchar(16) not null,
    started_at datetime not null,
    finished_at datetime null,
    success_count int not null default 0,
    failed_count int not null default 0,
    error_message varchar(1000) null,
    source varchar(32) null,
    actual_source varchar(32) null,
    fallback_reason varchar(500) null,
    request_params varchar(500) null,
    key idx_started_at (started_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

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
    observed_at datetime not null,
    key idx_snapshot_flight (flight_id, observed_at),
    key idx_snapshot_route (from_city, to_city, depart_time)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
