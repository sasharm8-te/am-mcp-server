package com.thousandeyes.cui.mcp.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data transfer object for organization information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationDto {
    
    private Long orgId;
    private String orgName;
    private Boolean cuiMigrationFlag;
    private Long createTime;
    private Long deleteTime;
    
    // CUI tenant mapping information
    private String cuiTenantId;
    private String cuiOrgId;
    private String cuiClusterUrl;
    private String mappingStatus;
    private Long mappingCreateTime;
    private Boolean cuiTenantControlEnabled;
    private Integer integrationStage;
    private Boolean phase2FeatureFlagEnabled;
}
