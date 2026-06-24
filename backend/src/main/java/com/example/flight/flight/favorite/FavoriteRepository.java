package com.example.flight.flight.favorite;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class FavoriteRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<FavoriteRecord> rowMapper = (rs, rowNum) -> new FavoriteRecord(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("flight_id"),
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
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    public FavoriteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FavoriteRecord create(Long userId, Long flightId) {
        Optional<FavoriteRecord> existing = findByUserIdAndFlightId(userId, flightId);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (!flightExists(flightId)) {
            throw new IllegalArgumentException("flightId does not reference an existing flight");
        }

        jdbcTemplate.update(
                "insert into favorite(user_id, flight_id, created_at) values (?, ?, ?)",
                userId,
                flightId,
                Timestamp.valueOf(LocalDateTime.now()));
        return findByUserIdAndFlightId(userId, flightId).orElseThrow();
    }

    public List<FavoriteRecord> findByUserId(Long userId) {
        return jdbcTemplate.query("""
                select
                    fav.id,
                    fav.user_id,
                    fav.flight_id,
                    fav.created_at,
                    f.flight_no,
                    f.airline_name,
                    f.from_city,
                    f.to_city,
                    f.from_airport,
                    f.to_airport,
                    f.depart_time,
                    f.arrive_time,
                    f.price,
                    f.seats_left,
                    f.data_source,
                    f.collected_at
                from favorite fav
                join flight f on f.id = fav.flight_id
                where fav.user_id = ?
                order by fav.created_at desc, fav.id desc
                """, rowMapper, userId);
    }

    public void deleteByUserAndFavoriteId(Long userId, Long favoriteId) {
        jdbcTemplate.update("delete from favorite where id = ? and user_id = ?", favoriteId, userId);
    }

    private Optional<FavoriteRecord> findByUserIdAndFlightId(Long userId, Long flightId) {
        List<FavoriteRecord> favorites = jdbcTemplate.query("""
                select
                    fav.id,
                    fav.user_id,
                    fav.flight_id,
                    fav.created_at,
                    f.flight_no,
                    f.airline_name,
                    f.from_city,
                    f.to_city,
                    f.from_airport,
                    f.to_airport,
                    f.depart_time,
                    f.arrive_time,
                    f.price,
                    f.seats_left,
                    f.data_source,
                    f.collected_at
                from favorite fav
                join flight f on f.id = fav.flight_id
                where fav.user_id = ? and fav.flight_id = ?
                order by fav.id desc
                """, rowMapper, userId, flightId);
        return favorites.stream().findFirst();
    }

    private boolean flightExists(Long flightId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from flight where id = ?",
                Integer.class,
                flightId);
        return count != null && count > 0;
    }
}
