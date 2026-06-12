package com.example.flight.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String hash = passwordEncoder.encode(request.password());
        User user = userRepository.insert(request.username(), hash, request.username());
        UserToken token = tokenRepository.createToken(user.id());
        log.info("用户注册: username={}", user.username());
        return new LoginResponse(user.id(), user.username(), user.nickname(), token.token());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.password())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        UserToken token = tokenRepository.createToken(user.id());
        log.info("用户登录: username={}", user.username());
        return new LoginResponse(user.id(), user.username(), user.nickname(), token.token());
    }

    public void logout(String token) {
        tokenRepository.deleteByToken(token);
        log.info("用户登出: token={}", token.substring(0, 8) + "...");
    }

    public Optional<User> validateToken(String token) {
        return tokenRepository.findByToken(token)
                .flatMap(t -> userRepository.findById(t.userId()));
    }

    public record LoginResponse(Long id, String username, String nickname, String token) {
    }
}
