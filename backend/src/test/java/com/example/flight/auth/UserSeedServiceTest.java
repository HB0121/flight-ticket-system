package com.example.flight.auth;

import com.example.flight.auth.dto.RegisterRequest;
import com.example.flight.auth.repository.UserRepository;
import com.example.flight.auth.service.AuthService;
import com.example.flight.auth.service.UserSeedService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSeedServiceTest {

    @Test
    void seedsAdminAndFlightDemoUsersWhenMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthService authService = mock(AuthService.class);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByUsername("flight")).thenReturn(false);

        UserSeedService service = new UserSeedService(userRepository, authService);
        service.seedAdminUser();

        verify(authService).register(new RegisterRequest("admin", "admin123"));
        verify(authService).register(new RegisterRequest("flight", "flight123"));
    }
}
