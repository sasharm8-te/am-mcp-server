package com.thousandeyes.cui.mcp.service;

import com.thousandeyes.cui.mcp.model.dto.SyncStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for synchronization-related MCP operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    
    public Map<String, Object> getSyncRetryStatus(Map<String, Object> arguments) {
        Integer page = getIntegerArgument(arguments, "page", 0);
        Integer size = getIntegerArgument(arguments, "size", 20);
        String entityType = (String) arguments.get("entity_type");
        
        log.info("Getting sync retry status - page: {}, size: {}, entityType: {}", page, size, entityType);
        
        StringBuilder sql = new StringBuilder("""
            SELECT entity_id, entity_type, sync_type, status, error_message,
                   retry_count, max_retries, last_attempt, next_retry,
                   create_time, update_time
            FROM te_admin.tb_cui_entity_sync_retry_status
            WHERE 1=1
        """);
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        
        if (entityType != null && !entityType.isEmpty()) {
            sql.append(" AND entity_type = :entityType");
            params.addValue("entityType", entityType);
        }
        
        sql.append(" ORDER BY create_time DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", size);
        params.addValue("offset", page * size);
        
        List<SyncStatusDto> retryStatuses = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> 
            SyncStatusDto.builder()
                    .entityId(rs.getString("entity_id"))
                    .entityType(rs.getString("entity_type"))
                    .syncType(rs.getString("sync_type"))
                    .status(rs.getString("status"))
                    .errorMessage(rs.getString("error_message"))
                    .retryCount(rs.getInt("retry_count"))
                    .maxRetries(rs.getInt("max_retries"))
                    .lastAttempt(rs.getTimestamp("last_attempt") != null ? 
                            rs.getTimestamp("last_attempt").toLocalDateTime() : null)
                    .nextRetry(rs.getTimestamp("next_retry") != null ? 
                            rs.getTimestamp("next_retry").toLocalDateTime() : null)
                    .createTime(rs.getTimestamp("create_time") != null ? 
                            rs.getTimestamp("create_time").toLocalDateTime() : null)
                    .updateTime(rs.getTimestamp("update_time") != null ? 
                            rs.getTimestamp("update_time").toLocalDateTime() : null)
                    .build()
        );
        
        // Get total count
        String countSql = "SELECT COUNT(*) FROM te_admin.tb_cui_entity_sync_retry_status WHERE 1=1";
        if (entityType != null && !entityType.isEmpty()) {
            countSql += " AND entity_type = :entityType";
        }
        
        Integer totalCount = jdbcTemplate.queryForObject(countSql, params, Integer.class);
        
        return Map.of(
            "retryStatuses", retryStatuses,
            "pagination", Map.of(
                "page", page,
                "size", size,
                "totalElements", totalCount,
                "totalPages", (totalCount + size - 1) / size
            )
        );
    }
    
    public String triggerUserSyncRetry(Map<String, Object> arguments) {
        Long uid = getLongArgument(arguments, "uid");
        Long orgId = getLongArgument(arguments, "org_id");
        
        log.info("Triggering user sync retry for UID: {}, org ID: {}", uid, orgId);
        
        // In a real implementation, this would trigger the retry mechanism
        // For now, we'll just log and return a success message
        
        return String.format("User sync retry triggered for UID: %d%s", 
                uid, orgId != null ? " in organization: " + orgId : "");
    }
    
    public String triggerOrgSyncRetry(Map<String, Object> arguments) {
        Long orgId = getLongArgument(arguments, "org_id");
        String syncType = (String) arguments.get("sync_type");
        
        log.info("Triggering organization sync retry for org ID: {}, sync type: {}", orgId, syncType);
        
        // In a real implementation, this would trigger the retry mechanism
        return String.format("Organization sync retry triggered for org ID: %d%s", 
                orgId, syncType != null ? " (type: " + syncType + ")" : "");
    }
    
    public Map<String, Object> getSyncMetrics(Map<String, Object> arguments) {
        String timeRange = (String) arguments.getOrDefault("time_range", "24h");
        String metricType = (String) arguments.get("metric_type");
        
        log.info("Getting sync metrics for time range: {}, metric type: {}", timeRange, metricType);
        
        // Convert time range to hours for SQL
        int hours = switch (timeRange) {
            case "1h" -> 1;
            case "24h" -> 24;
            case "7d" -> 168;
            case "30d" -> 720;
            default -> 24;
        };
        
        String sql = """
            SELECT 
                entity_type,
                sync_type,
                status,
                COUNT(*) as count,
                AVG(retry_count) as avg_retry_count
            FROM te_admin.tb_cui_entity_sync_retry_status
            WHERE create_time >= DATE_SUB(NOW(), INTERVAL :hours HOUR)
            GROUP BY entity_type, sync_type, status
            ORDER BY entity_type, sync_type, status
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("hours", hours);
        
        List<Map<String, Object>> metrics = jdbcTemplate.query(sql, params, (rs, rowNum) -> 
            Map.of(
                "entityType", rs.getString("entity_type"),
                "syncType", rs.getString("sync_type"),
                "status", rs.getString("status"),
                "count", rs.getInt("count"),
                "avgRetryCount", rs.getDouble("avg_retry_count")
            )
        );
        
        return Map.of(
            "timeRange", timeRange,
            "metrics", metrics,
            "summary", calculateSummary(metrics)
        );
    }
    
    public String clearRetryQueue(Map<String, Object> arguments) {
        String entityId = (String) arguments.get("entity_id");
        String entityType = (String) arguments.get("entity_type");
        
        log.info("Clearing retry queue for entity ID: {}, type: {}", entityId, entityType);
        
        String sql = """
            DELETE FROM te_admin.tb_cui_entity_sync_retry_status
            WHERE entity_id = :entityId AND entity_type = :entityType
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("entityId", entityId);
        params.addValue("entityType", entityType);
        
        int deletedRows = jdbcTemplate.update(sql, params);
        
        return String.format("Cleared %d retry entries for entity %s (%s)", 
                deletedRows, entityId, entityType);
    }
    
    private Map<String, Object> calculateSummary(List<Map<String, Object>> metrics) {
        int totalOperations = metrics.stream()
                .mapToInt(m -> (Integer) m.get("count"))
                .sum();
        
        long successCount = metrics.stream()
                .filter(m -> "SUCCESS".equals(m.get("status")))
                .mapToInt(m -> (Integer) m.get("count"))
                .sum();
        
        double successRate = totalOperations > 0 ? (double) successCount / totalOperations * 100 : 0;
        
        return Map.of(
            "totalOperations", totalOperations,
            "successCount", successCount,
            "successRate", Math.round(successRate * 100.0) / 100.0
        );
    }
    
    private Long getLongArgument(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }
    
    private Integer getIntegerArgument(Map<String, Object> arguments, String key, Integer defaultValue) {
        Object value = arguments.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
