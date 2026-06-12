package com.example.flight.config;

import com.example.flight.auth.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 全局 Web 配置类。
 * 实现 WebMvcConfigurer 接口以自定义 CORS 跨域规则和请求拦截器注册。
 *
 * 设计模式：组合优于继承 —— 通过实现接口而非继承 WebMvcConfigurerAdapter（已废弃）来定制 MVC 行为。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 登录拦截器，用于校验需要认证的接口请求 */
    private final LoginInterceptor loginInterceptor;

    /**
     * 构造器注入 LoginInterceptor。
     * Spring 自动发现类型匹配的 Bean 并注入。
     */
    public WebConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    /**
     * 配置 CORS（跨域资源共享）规则。
     * 允许前端（Vue 3 开发服务器，端口 5173）跨域访问 /api/** 路径。
     * 支持 GET / POST / PUT / DELETE / OPTIONS 方法，允许所有请求头。
     *
     * @param registry Spring MVC 提供的 CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 匹配所有以 /api/ 开头的接口
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173") // 仅允许前端开发服务器来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的 HTTP 方法
                .allowedHeaders("*"); // 允许所有请求头
    }

    /**
     * 注册请求拦截器。
     * 对需要登录的接口（采集触发、AI 对话、用户信息等）施加登录校验，
     * 同时排除登录和注册接口本身，避免死循环。
     *
     * @param registry Spring MVC 提供的拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/crawl/run", "/api/ai/conversations/**", "/api/auth/me", "/api/auth/logout") // 需要登录鉴权的路径
                .excludePathPatterns("/api/auth/login", "/api/auth/register"); // 排除免鉴权路径，否则用户永远无法登录
    }
}
