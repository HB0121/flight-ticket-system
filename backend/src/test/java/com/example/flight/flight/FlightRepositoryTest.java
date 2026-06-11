package com.example.flight.flight;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(FlightRepository.class)
class FlightRepositoryTest {
    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void searchesFlightsByRouteAndDateOrderedByPrice() {
        insertFlight("CA1502", "中国国航", "上海", "北京", "浦东机场", "大兴机场", "2026-06-19 11:20:00", "2026-06-19 13:35:00", 1280);
        insertFlight("MU5101", "东方航空", "上海", "北京", "虹桥机场", "首都机场", "2026-06-19 08:30:00", "2026-06-19 10:45:00", 980);
        insertFlight("ZH9103", "深圳航空", "深圳", "上海", "宝安机场", "虹桥机场", "2026-06-20 14:20:00", "2026-06-20 16:35:00", 720);

        var flights = flightRepository.search(new FlightSearchCriteria("上海", "北京", LocalDate.parse("2026-06-19")));

        assertThat(flights).extracting(Flight::flightNo).contains("MU5101", "CA1502");
        assertThat(flights.get(0).flightNo()).isEqualTo("MU5101");
    }

    @Test
    void searchesFlightsByDataSourceWhenProvided() {
        insertFlight("CA1502", "中国国航", "上海", "北京", "浦东机场", "大兴机场", "2026-06-19 11:20:00", "2026-06-19 13:35:00", 1280, "sample");
        insertFlight("MU5101", "东方航空", "上海", "北京", "浦东机场", "首都机场", "2026-06-19 08:30:00", "2026-06-19 10:45:00", 980, "amadeus");

        var flights = flightRepository.search(new FlightSearchCriteria("上海", "北京", LocalDate.parse("2026-06-19"), "amadeus"));

        assertThat(flights).extracting(Flight::flightNo).containsExactly("MU5101");
    }

    @Test
    void returnsPriceHistoryForFlightOrderedByObservationTime() {
        insertFlight("MU5101", "东方航空", "上海", "北京", "浦东机场", "首都机场", "2026-06-19 08:30:00", "2026-06-19 10:45:00", 980, "amadeus");
        Long flightId = jdbcTemplate.queryForObject("select id from flight where flight_no = 'MU5101' order by id desc limit 1", Long.class);
        jdbcTemplate.update("""
                insert into flight_price_snapshot(
                    flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, flightId, "MU5101", "上海", "北京", "2026-06-19 08:30:00", 1020, 9, "amadeus", "2026-06-11 10:00:00");
        jdbcTemplate.update("""
                insert into flight_price_snapshot(
                    flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, flightId, "MU5101", "上海", "北京", "2026-06-19 08:30:00", 980, 7, "amadeus", "2026-06-11 11:00:00");

        var history = flightRepository.findPriceHistory(flightId);

        assertThat(history).hasSize(2);
        assertThat(history).extracting(FlightPriceSnapshot::price).containsExactly(1020, 980);
        assertThat(history.get(1).observedAt()).isEqualTo(LocalDateTime.parse("2026-06-11T11:00:00"));
    }

    private void insertFlight(String flightNo,
                              String airlineName,
                              String fromCity,
                              String toCity,
                              String fromAirport,
                              String toAirport,
                              String departTime,
                              String arriveTime,
                              int price) {
        insertFlight(flightNo, airlineName, fromCity, toCity, fromAirport, toAirport, departTime, arriveTime, price, "sample");
    }

    private void insertFlight(String flightNo,
                              String airlineName,
                              String fromCity,
                              String toCity,
                              String fromAirport,
                              String toAirport,
                              String departTime,
                              String arriveTime,
                              int price,
                              String dataSource) {
        jdbcTemplate.update("""
                insert into flight(
                    flight_no, airline_name, from_city, to_city, from_airport, to_airport,
                    depart_time, arrive_time, price, seats_left, data_source, collected_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, flightNo, airlineName, fromCity, toCity, fromAirport, toAirport,
                departTime, arriveTime, price, 10, dataSource, "2026-06-11 10:00:00");
    }
}
