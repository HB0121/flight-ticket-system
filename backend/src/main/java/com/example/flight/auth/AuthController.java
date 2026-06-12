package com.example.flight.auth;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证 REST 控制器。
 * <p>
 * 提供用户注册、登录、登出和获取当前用户信息的 API 端点。
 * 所有端点均挂载在 {@code /api/auth} 路径下。
 * </p>
 *
 * <h3>API 端点一览</h3>
 * <table>
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>认证</th></tr>
 *   <tr><td>POST</td><td>/api/auth/register</td><td>用户注册</td><td>无需认证</td></tr>
 *   <tr><td>POST</td><td>/api/auth/login</td><td>用户登录</td><td>无需认证</td></tr>
 *   <tr><td>POST</td><td>/api/auth/logout</td><td>用户登出</td><td>需 Bearer Token</td></tr>
 *   <tr><td>GET</td><td>/api/auth/me</td><td>获取当前用户信息</td><td>通过拦截器注入</td></tr>
 * </table>
 *
 * <p>
 * 设计模式：Controller 层 —— 仅负责 HTTP 请求/响应转换，业务逻辑委托给 {@link AuthService}。
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** 认证服务，由构造函数注入 */
    private final AuthService authService;

    /**
     * 构造函数注入 AuthService。
     *
     * @param authService 认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册接口。
     * <p>
     * 接收注册请求，调用 AuthService 完成注册，
     * 成功返回 HTTP 201 Created 及用户信息和登录 token。
     * </p>
     *
     * @param request 经过 @Valid 校验的注册请求体
     * @return 201 Created + {id, username, nickname, token}
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.LoginResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", response.id(),
                "username", response.username(),
                "nickname", response.nickname(),
                "token", response.token()
        ));
    }

    /**
     * 用户登录接口。
     * <p>
     * 接收登录请求，校验用户名和密码，
     * 成功返回 HTTP 200 OK 及用户信息和登录 token。
     * 失败返回 400 Bad Request 及错误信息（由全局异常处理器处理）。
     * </p>
     *
     * @param request 经过 @Valid 校验的登录请求体
     * @return 200 OK + {id, username, nickname, token}
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(Map.of(
                "id", response.id(),
                "username", response.username(),
                "nickname", response.nickname(),
                "token", response.token()
        ));
    }

    /**
     * 用户登出接口。
     * <p>
     * 从 Authorization 请求头提取 Bearer token，调用 AuthService 删除令牌。
     * 无论 token 是否有效，均返回 204 No Content（不暴露用户状态）。
     * Authorization 头为可选参数，不存在时静默跳过。
     * </p>
     *
     * @param authHeader Authorization 请求头（格式：Bearer <token>），可选
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 截取 "Bearer " 之后的 token 部分（7 个字符）
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取当前登录用户信息。
     * <p>
     * 用户对象由 {@link LoginInterceptor} 在拦截阶段注入到 request 属性中。
     * 如果 user 为 null，说明请求未经过拦截器或 token 无效，返回 401。
     * 注意：此端点需要在拦截器白名单之外独立配置，或通过 WebConfig 放行后手动校验。
     * </p>
     *
     * @param user 由拦截器注入的用户对象（RequestAttribute），可能为 null
     * @return 200 OK + {id, username, nickname} 或 401 Unauthorized
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestAttribute(value = "user", required = false) User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "未登录或 token 已过期"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "id", user.id(),
                "username", user.username(),
                "nickname", user.nickname()
        ));
    }
}
