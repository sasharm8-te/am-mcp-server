package com.thousandeyes.cui.mcp.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data transfer object for synchronization status information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncStatusDto {
    
    private String entityId;
    private String entityType;
    private String syncType;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime lastAttempt;
    private LocalDateTime nextRetry;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
