package com.competition.invoice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数仓独立数据源配置
 * 仅在配置了 warehouse.jdbc-url 时才生效
 */
@Configuration
public class WarehouseConfig {

    @Bean
    @ConditionalOnProperty(prefix = "external.warehouse", name = "jdbc-url")
    @ConfigurationProperties(prefix = "external.warehouse")
    public HikariDataSource warehouseDataSource() {
        return new HikariDataSource();
    }
}
