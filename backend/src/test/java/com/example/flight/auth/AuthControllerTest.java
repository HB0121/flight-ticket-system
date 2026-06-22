package com.example.flight.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void loginReturnsExplicitSessionResponseWithExistingJsonFields() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        LoginRequest request = new LoginRequest("alice", "secret");
        when(authService.login(request)).thenReturn(new AuthService.LoginResponse(7L, "alice", "Alice", "token-1"));

        ResponseEntity<?> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new AuthSessionResponse(7L, "alice", "Alice", "token-1"));
    }

    @Test
    void registerReturnsExplicitSessionResponseWithCreatedStatus() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        RegisterRequest request = new RegisterRequest("alice", "secret");
        when(authService.register(request)).thenReturn(new AuthService.LoginResponse(7L, "alice", "Alice", "token-1"));

        ResponseEntity<?> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(new AuthSessionResponse(7L, "alice", "Alice", "token-1"));
    }

    @Test
    void meReturnsExplicitCurrentUserResponseWhenAuthenticated() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        User user = new User(7L, "alice", "hash", "Alice", LocalDateTime.parse("2026-06-22T10:00:00"));

        ResponseEntity<?> response = controller.me(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new MeResponse(7L, "alice", "Alice"));
    }

    @Test
    void meReturnsExplicitErrorResponseWhenUnauthenticated() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);

        ResponseEntity<?> response = controller.me(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(AuthErrorResponse.class);
    }
}
