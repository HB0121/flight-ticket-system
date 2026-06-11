package com.example.flight.flight;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class FlightRepository implements FlightSearchPort {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Flight> rowMapper = (rs, rowNum) -> new Flight(
            rs.getLong("id"),
            rs.getString("flight_no"),
            rs.getString("airline_name"),
            rs.getString("from_city"),
            rs.getString("to_city"),
            rs.getString("from_airport"),
            rs.getString("to_airport"),
            rs.getTimestamp("depart_time").toLocalDateTime(),
            rs.getTimestamp("arrive_time").toLocalDateTime(),
            rs.getBigDecimal("price"),
            rs.getInt("seats_left"),
            rs.getString("data_source"),
            rs.getTimestamp("collected_at").toLocalDateTime()
    );

    public FlightRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Flight> search(FlightSearchCriteria criteria) {
        var sql = new StringBuilder("select * from flight where 1=1");
        var args = new ArrayList<Object>();

        if (StringUtils.hasText(criteria.fromCity())) {
            sql.append(" and from_city = ?");
            args.add(criteria.fromCity());
        }
        if (StringUtils.hasText(criteria.toCity())) {
            sql.append(" and to_city = ?");
            args.add(criteria.toCity());
        }
        if (criteria.date() != null) {
            LocalDate date = criteria.date();
            sql.append(" and depart_time >= ? and depart_time < ?");
            args.add(Timestamp.valueOf(date.atStartOfDay()));
            args.add(Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
        }

        sql.append(" order by price asc, depart_time asc");
        return jdbcTemplate.query(sql.toString(), rowMapper, args.toArray());
    }

    public Optional<Flight> findById(Long id) {
        var flights = jdbcTemplate.query("select * from flight where id = ?", rowMapper, id);
        return flights.stream().findFirst();
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("select count(*) from flight", Long.class);
        return count == null ? 0 : count;
    }
}

