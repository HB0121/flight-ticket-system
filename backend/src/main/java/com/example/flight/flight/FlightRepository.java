package com.example.flight.flight;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class FlightRepository implements FlightSearchPort, PriceHistoryPort {
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
            rs.getTimestamp("collected_at").toLocalDateTime(),
            getBooleanOrNull(rs, "is_favorited"),
            getLongOrNull(rs, "favorite_id")
    );

    private final RowMapper<FlightPriceSnapshot> snapshotRowMapper = (rs, rowNum) -> new FlightPriceSnapshot(
            rs.getLong("id"),
            rs.getLong("flight_id"),
            rs.getString("flight_no"),
            rs.getString("from_city"),
            rs.getString("to_city"),
            rs.getTimestamp("depart_time").toLocalDateTime(),
            rs.getInt("price"),
            rs.getInt("seats_left"),
            rs.getString("data_source"),
            rs.getTimestamp("observed_at").toLocalDateTime()
    );

    private final RowMapper<Flight> rowMapperWithFavorite = (rs, rowNum) -> new Flight(
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
            rs.getTimestamp("collected_at").toLocalDateTime(),
            getBooleanOrNull(rs, "is_favorited"),
            getLongOrNull(rs, "favorite_id")
    );

    public FlightRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Flight> search(FlightSearchCriteria criteria) {
        var sql = new StringBuilder("select * from flight where 1=1");
        var args = new ArrayList<Object>();

        if (StringUtils.hasText(criteria.fromCity())) {
            sql.append(" and (from_city = ? or upper(from_airport) = upper(?))");
            args.add(criteria.fromCity());
            args.add(criteria.fromCity());
        }
        if (StringUtils.hasText(criteria.toCity())) {
            sql.append(" and (to_city = ? or upper(to_airport) = upper(?))");
            args.add(criteria.toCity());
            args.add(criteria.toCity());
        }
        if (criteria.date() != null) {
            LocalDate date = criteria.date();
            sql.append(" and depart_time >= ? and depart_time < ?");
            args.add(Timestamp.valueOf(date.atStartOfDay()));
            args.add(Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
        }
        if (StringUtils.hasText(criteria.dataSource())) {
            sql.append(" and data_source = ?");
            args.add(criteria.dataSource());
        }

        sql.append(" order by price asc, depart_time asc");
        return jdbcTemplate.query(sql.toString(), rowMapper, args.toArray());
    }

    public List<Flight> search(FlightSearchCriteria criteria, Long userId) {
        var sql = new StringBuilder(
            "SELECT f.*, " +
            "fav.id AS favorite_id, " +
            "CASE WHEN fav.id IS NOT NULL THEN true ELSE false END AS is_favorited " +
            "FROM flight f " +
            "LEFT JOIN favorite fav ON fav.flight_id = f.id AND fav.user_id = ? " +
            "WHERE 1=1");
        var args = new ArrayList<Object>();
        args.add(userId);

        if (StringUtils.hasText(criteria.fromCity())) {
            sql.append(" and (f.from_city = ? or upper(f.from_airport) = upper(?))");
            args.add(criteria.fromCity());
            args.add(criteria.fromCity());
        }
        if (StringUtils.hasText(criteria.toCity())) {
            sql.append(" and (f.to_city = ? or upper(f.to_airport) = upper(?))");
            args.add(criteria.toCity());
            args.add(criteria.toCity());
        }
        if (criteria.date() != null) {
            LocalDate date = criteria.date();
            sql.append(" and f.depart_time >= ? and f.depart_time < ?");
            args.add(Timestamp.valueOf(date.atStartOfDay()));
            args.add(Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
        }
        if (StringUtils.hasText(criteria.dataSource())) {
            sql.append(" and f.data_source = ?");
            args.add(criteria.dataSource());
        }

        sql.append(" order by f.price asc, f.depart_time asc");
        return jdbcTemplate.query(sql.toString(), rowMapperWithFavorite, args.toArray());
    }

    public Optional<Flight> findById(Long id) {
        var flights = jdbcTemplate.query("select * from flight where id = ?", rowMapper, id);
        return flights.stream().findFirst();
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("select count(*) from flight", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<FlightPriceSnapshot> findPriceHistory(Long flightId) {
        return jdbcTemplate.query(
                "select * from flight_price_snapshot where flight_id = ? order by observed_at asc",
                snapshotRowMapper, flightId);
    }

    private Boolean getBooleanOrNull(ResultSet rs, String column) throws SQLException {
        try {
            boolean val = rs.getBoolean(column);
            return rs.wasNull() ? null : val;
        } catch (SQLException e) {
            return null;
        }
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        try {
            long val = rs.getLong(column);
            return rs.wasNull() ? null : val;
        } catch (SQLException e) {
            return null;
        }
    }
}
