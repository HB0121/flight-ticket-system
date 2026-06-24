package com.example.flight.flight.history;

import com.example.flight.auth.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(SearchHistoryRepository.class)
@TestPropertySource(properties = "spring.sql.init.schema-locations=classpath:schema.sql")
class SearchHistoryRepositoryTest {

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appendsAndReadsHistoryByUserMostRecentFirst() {
        User user = insertUser("history-user");

        searchHistoryRepository.append(user.id(), new SearchHistoryCommand("Shanghai", "Beijing", LocalDate.parse("2026-06-19"), "sample"));
        searchHistoryRepository.append(user.id(), new SearchHistoryCommand("Shanghai", "Shenzhen", LocalDate.parse("2026-06-20"), null));

        assertThat(searchHistoryRepository.findByUserId(user.id()))
                .extracting(SearchHistoryRecord::toCity)
                .containsExactly("Shenzhen", "Beijing");
    }

    @Test
    void limitsHistoryResultsToMostRecentItems() {
        User user = insertUser("history-limit");

        for (int index = 0; index < 4; index++) {
            searchHistoryRepository.append(
                    user.id(),
                    new SearchHistoryCommand("City-" + index, "Dest-" + index, null, null));
        }

        assertThat(searchHistoryRepository.findByUserId(user.id(), 3))
                .extracting(SearchHistoryRecord::fromCity)
                .containsExactly("City-3", "City-2", "City-1");
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
}
