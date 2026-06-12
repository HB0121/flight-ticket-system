package com.example.flight.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 默认用户种子服务。
 * <p>
 * 在应用启动完成后自动执行，确保系统中至少存在一个管理员账号。
 * 仅在 admin 用户不存在时才创建，避免重复插入。
 * </p>
 *
 * <h3>设计动机</h3>
 * <p>
 * 用于本地课堂演示场景：教师启动系统后即可使用预设账号登录，
 * 无需手动执行 SQL 脚本或注册流程。默认凭据为 admin / admin123。
 * </p>
 *
 * <p>
 * 设计模式：事件监听器（Event Listener）模式 —— 监听 Spring Boot 的
 * {@link ApplicationReadyEvent}，在应用完全就绪后执行初始化逻辑。
 * </p>
 */
@Component
public class UserSeedService {

    private static final Logger log = LoggerFactory.getLogger(UserSeedService.class);

    /** 用户数据仓库 */
    private final UserRepository userRepository;
    /** 认证服务（用于注册新用户） */
    private final AuthService authService;

    /**
     * 构造函数注入依赖。
     *
     * @param userRepository 用户数据仓库
     * @param authService    认证服务
     */
    public UserSeedService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * 应用就绪后执行：创建默认管理员账号。
     * <p>
     * 监听 {@link ApplicationReadyEvent}，该事件在应用完全启动、
     * 所有 Bean 初始化完成后触发。先检查 admin 用户是否已存在，
     * 若不存在则通过 AuthService 注册新用户（密码经 BCrypt 哈希后存储）。
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            // 使用 AuthService 注册，自动完成 BCrypt 哈希和 token 生成
            authService.register(new RegisterRequest("admin", "admin123"));
            log.info("已创建默认管理员账号: admin/admin123");
        }
    }
}
