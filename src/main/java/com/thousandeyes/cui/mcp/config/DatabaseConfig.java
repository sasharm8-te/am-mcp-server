package com.thousandeyes.cui.mcp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Database configuration for connecting to the CUI Integration Service database.
 */
@Configuration
@ConfigurationProperties(prefix = "database")
@Data
public class DatabaseConfig {
    
    private String url;
    private String username;
    private String password;
    private String driverClassName = "com.mysql.cj.jdbc.Driver";
    private ConnectionPool connectionPool = new ConnectionPool();
    
    @Data
    public static class ConnectionPool {
        private int maximumPoolSize = 10;
        private int minimumIdle = 2;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
    }
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(connectionPool.getMaximumPoolSize());
        config.setMinimumIdle(connectionPool.getMinimumIdle());
        config.setConnectionTimeout(connectionPool.getConnectionTimeout());
        config.setIdleTimeout(connectionPool.getIdleTimeout());
        config.setMaxLifetime(connectionPool.getMaxLifetime());
        
        // Connection pool settings
        config.setPoolName("CUI-MCP-Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return new HikariDataSource(config);
    }
    
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
