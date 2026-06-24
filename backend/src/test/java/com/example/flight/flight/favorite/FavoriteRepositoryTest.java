package com.example.flight.flight.favorite;

import com.example.flight.auth.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(FavoriteRepository.class)
@TestPropertySource(properties = "spring.sql.init.schema-locations=classpath:schema.sql")
class FavoriteRepositoryTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsListsAndDeletesFavoritesByUser() {
        User user = insertUser("alice");
        Long flightId = insertFlight("MU1001");

        FavoriteRecord created = favoriteRepository.create(user.id(), flightId);

        assertThat(created.userId()).isEqualTo(user.id());
        assertThat(created.flightId()).isEqualTo(flightId);
        assertThat(favoriteRepository.findByUserId(user.id()))
                .extracting(FavoriteRecord::flightId)
                .containsExactly(flightId);

        favoriteRepository.deleteByUserAndFavoriteId(user.id(), created.id());

        assertThat(favoriteRepository.findByUserId(user.id())).isEmpty();
    }

    @Test
    void ignoresDuplicateFavoriteForSameUserAndFlight() {
        User user = insertUser("bob");
        Long flightId = insertFlight("CA2002");

        FavoriteRecord first = favoriteRepository.create(user.id(), flightId);
        FavoriteRecord second = favoriteRepository.create(user.id(), flightId);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(favoriteRepository.findByUserId(user.id())).hasSize(1);
    }

    @Test
    void rejectsUnknownFlightBeforeInsert() {
        User user = insertUser("charlie");

        assertThatThrownBy(() -> favoriteRepository.create(user.id(), 99999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("flight");

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from favorite where user_id = ?",
                Integer.class,
                user.id());
        assertThat(count).isZero();
    }

    private User insertUser(String username) {
        jdbcTemplate.update(
                "insert into app_user(username, password, nickname, created_at) values (?, ?, ?, ?)",
                username, "hash", username, LocalDateTime.parse("2026-06-17T09:00:00"));
        Long id = jdbcTemplate.queryForObject(
                "select id from app_user where username = ?",
                Long.class,
                username);
        return new User(id, username, "hash", username, LocalDateTime.parse("2026-06-17T09:00:00"));
    }

    private Long insertFlight(String flightNo) {
        jdbcTemplate.update("""
                insert into flight(
                    flight_no, airline_name, from_city, to_city, from_airport, to_airport,
                    depart_time, arrive_time, price, seats_left, data_source, collected_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                flightNo, "Demo Air", "Shanghai", "Beijing", "PVG", "PEK",
                "2026-06-19 08:00:00", "2026-06-19 10:00:00", 880, 9, "sample", "2026-06-17 09:00:00");
        return jdbcTemplate.queryForObject(
                "select id from flight where flight_no = ? order by id desc limit 1",
                Long.class,
                flightNo);
    }
}
