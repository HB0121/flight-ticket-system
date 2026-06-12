package com.example.flight.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * 登录拦截器 —— Spring MVC 拦截器。
 * <p>
 * 在请求到达 Controller 之前执行，验证请求是否携带有效的认证令牌。
 * 验证通过后将用户对象注入到 request 属性中，Controller 可通过
 * {@code @RequestAttribute("user")} 获取当前登录用户。
 * </p>
 *
 * <h3>拦截流程</h3>
 * <ol>
 *   <li>CORS 预检请求（OPTIONS）直接放行，不做认证。</li>
 *   <li>从 Authorization 请求头提取 Bearer token。</li>
 *   <li>调用 AuthService 验证 token 有效性和过期状态。</li>
 *   <li>有效：将 User 对象写入 request 属性，返回 true 放行。</li>
 *   <li>无效/过期：返回 401 JSON 错误响应，拦截请求。</li>
 * </ol>
 *
 * <p>
 * 设计模式：拦截器（Interceptor）模式 —— 在请求处理链中插入横切关注点（认证），
 * 与业务 Controller 解耦。需在 WebConfig 中注册并配置拦截路径。
 * </p>
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoginInterceptor.class);

    /** 认证服务，由构造函数注入 */
    private final AuthService authService;

    /**
     * 构造函数注入 AuthService。
     *
     * @param authService 认证服务
     */
    public LoginInterceptor(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 请求前置处理 —— 在 Controller 方法执行前调用。
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象（用于写入 401 错误）
     * @param handler  目标处理器
     * @return true 放行请求，false 拦截请求并返回 401
     * @throws Exception 写入响应时的 IO 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 放行 CORS 预检请求，浏览器在跨域请求前会先发送 OPTIONS 请求探测
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 检查 Authorization 请求头是否存在且格式正确
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 未携带 token，返回 401 未登录
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":401,\"error\":\"未登录\",\"message\":\"请先登录后再操作\"}");
            return false;
        }

        // 提取 token（跳过 "Bearer " 前缀，共 7 个字符）
        String token = authHeader.substring(7);
        // 验证 token 有效性（含过期检查）
        Optional<User> user = authService.validateToken(token);
        if (user.isEmpty()) {
            // token 无效或已过期，返回 401
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":401,\"error\":\"登录已过期\",\"message\":\"token 无效或已过期，请重新登录\"}");
            return false;
        }

        // 验证通过，将用户对象注入 request 属性，供 Controller 使用
        request.setAttribute("user", user.get());
        return true;
    }
}
