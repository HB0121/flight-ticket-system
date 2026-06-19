package com.example.flight.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationDatasourceConfigTest {

    @Test
    void defaultDatasourceTargetsInfraMysql() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://localhost:3306/flight_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        assertThat(properties.getProperty("spring.datasource.username")).isEqualTo("flight");
        assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("flight123");
        assertThat(properties.getProperty("spring.datasource.driver-class-name")).isEqualTo("com.mysql.cj.jdbc.Driver");
    }
}
