package com.example.flight.flight.favorite;

import com.example.flight.auth.User;
import com.example.flight.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FavoriteControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FavoriteService favoriteService = mock(FavoriteService.class);
        FavoriteController controller = new FavoriteController(favoriteService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsMissingFlightId() throws Exception {
        User user = new User(1L, "alice", "hash", "Alice", LocalDateTime.parse("2026-06-17T09:00:00"));

        mockMvc.perform(post("/api/me/favorites")
                        .requestAttr("user", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
