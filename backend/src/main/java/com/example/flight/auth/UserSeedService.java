package com.example.flight.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserSeedService {

    private static final Logger log = LoggerFactory.getLogger(UserSeedService.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserSeedService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedAdminUser() {
        seedUser("admin", "admin123", "默认管理员账号");
        seedUser("flight", "flight123", "默认演示账号");
    }

    private void seedUser(String username, String password, String label) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        authService.register(new RegisterRequest(username, password));
        log.info("已创建{}: {}/{}", label, username, password);
    }
}
