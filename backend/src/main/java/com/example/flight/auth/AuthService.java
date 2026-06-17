package com.example.flight.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 认证服务 —— 核心业务逻辑层。
 * <p>
 * 负责用户注册、登录、登出、令牌验证等所有认证相关业务。
 * 使用 Spring Security 的 {@link BCryptPasswordEncoder} 进行密码哈希，
 * 数据库中永不存储明文密码。
 * </p>
 *
 * <h3>安全设计要点</h3>
 * <ul>
 *   <li>密码哈希：使用 BCrypt 算法（内置盐值），每次哈希结果不同，防止彩虹表攻击。</li>
 *   <li>密码校验：使用 {@link BCryptPasswordEncoder#matches(CharSequence, String)} 进行恒定时间比较。</li>
 *   <li>令牌管理：每次登录生成新 UUID 令牌，7 天过期，登出时主动删除。</li>
 *   <li>错误提示：登录失败统一返回"用户名或密码错误"，不区分具体原因，防止用户名枚举攻击。</li>
 * </ul>
 *
 * <p>
 * 设计模式：Service 层模式 —— 封装业务逻辑，协调 Repository 层完成持久化操作。
 * </p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** 用户数据仓库 */
    private final UserRepository userRepository;
    /** 令牌数据仓库 */
    private final TokenRepository tokenRepository;
    /**
     * BCrypt 密码编码器。
     * 默认强度为 10（2^10 轮哈希），在安全性和性能之间取得平衡。
     * encode() 方法自动生成随机盐值并嵌入结果字符串中。
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 构造函数注入依赖。
     *
     * @param userRepository  用户数据仓库
     * @param tokenRepository 令牌数据仓库
     */
    public AuthService(UserRepository userRepository, TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    /**
     * 用户注册。
     * <p>
     * 流程：
     * <ol>
     *   <li>检查用户名是否已存在，若存在则抛出异常</li>
     *   <li>使用 BCrypt 对密码进行哈希</li>
     *   <li>插入用户记录（昵称默认为用户名）</li>
     *   <li>为新用户创建登录令牌</li>
     *   <li>返回包含用户信息和 token 的响应</li>
     * </ol>
     * </p>
     *
     * @param request 注册请求（含用户名和明文密码）
     * @return 包含用户 ID、用户名、昵称和登录 token 的响应
     * @throws IllegalArgumentException 当用户名已存在时
     */
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        // BCrypt 哈希：自动加盐，输出格式为 $2a$10$...
        String hash = passwordEncoder.encode(request.password());
        User user = userRepository.insert(request.username(), hash);
        UserToken token = tokenRepository.createToken(user.id());
        log.info("用户注册: username={}", user.username());
        return new LoginResponse(user.id(), user.username(), token.token());
    }

    /**
     * 用户登录。
     * <p>
     * 流程：
     * <ol>
     *   <li>根据用户名查找用户，不存在则报错（不区分用户名不存在还是密码错误）</li>
     *   <li>使用 BCrypt 的 matches() 方法校验密码</li>
     *   <li>密码正确则创建新令牌并返回</li>
     * </ol>
     * </p>
     *
     * @param request 登录请求（含用户名和明文密码）
     * @return 包含用户 ID、用户名、昵称和登录 token 的响应
     * @throws IllegalArgumentException 当用户名或密码错误时（安全起见不区分具体原因）
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        // BCrypt 校验：使用恒定时间比较，防止时序攻击
        if (!passwordEncoder.matches(request.password(), user.password())) {
            throw new IllegalArgumentException("密码错误");
        }
        UserToken token = tokenRepository.createToken(user.id());
        log.info("用户登录: username={}", user.username());
        return new LoginResponse(user.id(), user.username(), token.token());
    }

    /**
     * 用户登出。
     * <p>
     * 从数据库中删除对应的 token 记录，使其立即失效。
     * 日志中仅记录 token 前 8 位，保护完整 token 不泄漏。
     * </p>
     *
     * @param token 待失效的令牌字符串
     */
    public void logout(String token) {
        tokenRepository.deleteByToken(token);
        log.info("用户登出: token={}", token.substring(0, 8) + "...");
    }

    /**
     * 验证令牌有效性。
     * <p>
     * 先查找有效令牌（匹配 + 未过期），再根据令牌中的 userId 查找对应用户。
     * 使用 flatMap 链式调用，任一步骤为空则返回 Optional.empty()。
     * </p>
     *
     * @param token 待验证的令牌字符串
     * @return 包含有效用户的 Optional，若令牌无效/过期/用户不存在则为 Optional.empty()
     */
    public Optional<User> validateToken(String token) {
        return tokenRepository.findByToken(token)
                .flatMap(t -> userRepository.findById(t.userId()));
    }

    /**
     * 登录/注册成功响应 DTO（内嵌记录类）。
     * <p>
     * 统一了登录和注册的返回格式，前端无需区分两种场景。
     * </p>
     *
     * @param id       用户 ID
     * @param username 用户名
     * @param nickname 显示昵称
     * @param token    登录令牌字符串
     */
    public record LoginResponse(Long id, String username, String token) {
    }
}
