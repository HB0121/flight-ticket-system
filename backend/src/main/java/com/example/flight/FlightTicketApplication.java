package com.example.flight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot 应用主入口类。
 * 整个航班票务系统的启动点，通过 main 方法引导 Spring 容器初始化。
 *
 * @SpringBootApplication 是一个组合注解，等价于：
 *   - @SpringBootConfiguration（标记为配置类）
 *   - @EnableAutoConfiguration（自动装配 SpringBoot 生态组件）
 *   - @ComponentScan（扫描当前包及子包下的 Bean）
 */
@SpringBootApplication
public class FlightTicketApplication {
    /**
     * JVM 入口方法，启动嵌入式 Tomcat 并初始化整个 Spring 容器。
     *
     * @param args 命令行参数（极少使用，配置由 application.yml 驱动）
     */
    public static void main(String[] args) {
        SpringApplication.run(FlightTicketApplication.class, args);
    }
}
