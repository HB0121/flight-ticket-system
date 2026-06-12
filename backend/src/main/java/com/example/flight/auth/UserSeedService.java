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
        if (!userRepository.existsByUsername("admin")) {
            authService.register(new RegisterRequest("admin", "admin123"));
            log.info("已创建默认管理员账号: admin/admin123");
        }
    }
}
