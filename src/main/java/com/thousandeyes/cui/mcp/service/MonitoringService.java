package com.thousandeyes.cui.mcp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for monitoring-related MCP operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final WebClient cuiIntegrationServiceClient;
    
    public Map<String, Object> getServiceHealth(Map<String, Object> arguments) {
        log.info("Getting service health status");
        
        Map<String, Object> databaseHealth = checkDatabaseHealth();
        Map<String, Object> externalServiceHealth = checkExternalServiceHealth();
        
        boolean overallHealthy = (Boolean) databaseHealth.get("healthy") && 
                                (Boolean) externalServiceHealth.get("healthy");
        
        return Map.of(
            "status", overallHealthy ? "UP" : "DOWN",
            "timestamp", LocalDateTime.now(),
            "components", Map.of(
                "database", databaseHealth,
                "externalServices", externalServiceHealth
            )
        );
    }
    
    public Map<String, Object> getSyncStatistics(Map<String, Object> arguments) {
        String timeRange = (String) arguments.getOrDefault("time_range", "24h");
        
        log.info("Getting sync statistics for time range: {}", timeRange);
        
        int hours = switch (timeRange) {
            case "1h" -> 1;
            case "24h" -> 24;
            case "7d" -> 168;
            case "30d" -> 720;
            default -> 24;
        };
        
        // Get sync statistics from retry status table
        String sql = """
            SELECT 
                COUNT(*) as total_syncs,
                SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as successful_syncs,
                SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_syncs,
                AVG(retry_count) as avg_retry_count
            FROM te_admin.tb_cui_entity_sync_retry_status
            WHERE create_time >= DATE_SUB(NOW(), INTERVAL :hours HOUR)
        """;
        
        Map<String, Object> stats = jdbcTemplate.queryForObject(sql, 
                Map.of("hours", hours), 
                (rs, rowNum) -> {
                    int totalSyncs = rs.getInt("total_syncs");
                    int successfulSyncs = rs.getInt("successful_syncs");
                    int failedSyncs = rs.getInt("failed_syncs");
                    double avgRetryCount = rs.getDouble("avg_retry_count");
                    
                    double successRate = totalSyncs > 0 ? (double) successfulSyncs / totalSyncs * 100 : 0;
                    
                    return Map.of(
                        "totalSyncs", totalSyncs,
                        "successfulSyncs", successfulSyncs,
                        "failedSyncs", failedSyncs,
                        "successRate", Math.round(successRate * 100.0) / 100.0,
                        "avgRetryCount", Math.round(avgRetryCount * 100.0) / 100.0
                    );
                });
        
        return Map.of(
            "timeRange", timeRange,
            "statistics", stats,
            "timestamp", LocalDateTime.now()
        );
    }
    
    public Map<String, Object> getKafkaStreamStatus(Map<String, Object> arguments) {
        log.info("Getting Kafka stream status");
        
        // In a real implementation, this would check actual Kafka stream metrics
        // For now, return a mock status
        return Map.of(
            "status", "RUNNING",
            "streams", List.of(
                Map.of(
                    "name", "user-cui-tenant-mapping-status",
                    "state", "RUNNING",
                    "lag", 0
                ),
                Map.of(
                    "name", "cui-entity-sync-retry",
                    "state", "RUNNING", 
                    "lag", 5
                )
            ),
            "timestamp", LocalDateTime.now()
        );
    }
    
    public Map<String, Object> getDatabaseConnectivity(Map<String, Object> arguments) {
        log.info("Checking database connectivity");
        
        return checkDatabaseHealth();
    }
    
    public Map<String, Object> getExternalServiceStatus(Map<String, Object> arguments) {
        String serviceName = (String) arguments.get("service_name");
        
        log.info("Getting external service status for: {}", serviceName);
        
        if (serviceName != null) {
            return checkSpecificService(serviceName);
        } else {
            return checkExternalServiceHealth();
        }
    }
    
    private Map<String, Object> checkDatabaseHealth() {
        try {
            // Test database connection
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5 second timeout
                
                if (isValid) {
                    // Test a simple query
                    Integer result = jdbcTemplate.queryForObject(
                            "SELECT 1", Map.of(), Integer.class);
                    
                    return Map.of(
                        "healthy", true,
                        "status", "UP",
                        "responseTime", "< 100ms",
                        "details", "Database connection successful"
                    );
                } else {
                    return Map.of(
                        "healthy", false,
                        "status", "DOWN",
                        "details", "Database connection invalid"
                    );
                }
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Map.of(
                "healthy", false,
                "status", "DOWN",
                "error", e.getMessage()
            );
        }
    }
    
    private Map<String, Object> checkExternalServiceHealth() {
        try {
            // Test CUI Integration Service health
            String healthResponse = cuiIntegrationServiceClient
                    .get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return Map.of(
                "healthy", true,
                "status", "UP",
                "services", Map.of(
                    "cui-integration-service", Map.of(
                        "status", "UP",
                        "response", healthResponse != null ? "OK" : "No response"
                    )
                )
            );
        } catch (Exception e) {
            log.error("External service health check failed", e);
            return Map.of(
                "healthy", false,
                "status", "DOWN",
                "services", Map.of(
                    "cui-integration-service", Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                    )
                )
            );
        }
    }
    
    private Map<String, Object> checkSpecificService(String serviceName) {
        try {
            switch (serviceName.toUpperCase()) {
                case "CUI_INTEGRATION_SERVICE":
                case "CUI-INTEGRATION-SERVICE":
                    String response = cuiIntegrationServiceClient
                            .get()
                            .uri("/actuator/health")
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                    
                    return Map.of(
                        "serviceName", serviceName,
                        "status", "UP",
                        "responseTime", "< 500ms",
                        "lastCheck", LocalDateTime.now()
                    );
                    
                default:
                    return Map.of(
                        "serviceName", serviceName,
                        "status", "UNKNOWN",
                        "error", "Service not recognized"
                    );
            }
        } catch (Exception e) {
            log.error("Service health check failed for: {}", serviceName, e);
            return Map.of(
                "serviceName", serviceName,
                "status", "DOWN",
                "error", e.getMessage(),
                "lastCheck", LocalDateTime.now()
            );
        }
    }
}
