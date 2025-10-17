package com.thousandeyes.cui.mcp.service;

import com.thousandeyes.cui.mcp.model.dto.OrganizationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Service for organization-related MCP operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final WebClient cuiIntegrationServiceClient;
    
    public OrganizationDto getOrganizationDetails(Map<String, Object> arguments) {
        Long orgId = getLongArgument(arguments, "org_id");
        
        log.info("Getting organization details for org ID: {}", orgId);
        
        String sql = """
            SELECT o.org_id, o.organization_name, o.flag_cui_migrated, o.date_create, o.delete_time,
                   ctm.cui_tenant_id, ctm.cui_org_id, ctm.cui_cluster_url, ctm.status as mapping_status,
                   ctm.created_at as mapping_create_time
            FROM te_admin.tb_organizations o
            LEFT JOIN te_admin.tb_organization_cui_tenant_mapping ctm ON o.org_id = ctm.org_id
            WHERE o.org_id = :orgId
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("orgId", orgId);
        
        OrganizationDto organization = jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> 
            OrganizationDto.builder()
                    .orgId(rs.getLong("org_id"))
                    .orgName(rs.getString("organization_name"))
                    .cuiMigrationFlag(rs.getBoolean("flag_cui_migrated"))
                    .createTime(rs.getLong("date_create"))
                    .deleteTime(rs.getLong("delete_time"))
                    .cuiTenantId(rs.getString("cui_tenant_id"))
                    .cuiOrgId(rs.getString("cui_org_id"))
                    .cuiClusterUrl(rs.getString("cui_cluster_url"))
                    .mappingStatus(rs.getString("mapping_status"))
                    .mappingCreateTime(rs.getLong("mapping_create_time"))
                    .build()
        );
        
        // Check for Phase 2 feature flag
        enrichWithFeatureFlag(organization);
        return organization;
    }
    
    public Object getCuiTenantDetails(Map<String, Object> arguments) {
        Long orgId = getLongArgument(arguments, "org_id");
        
        log.info("Getting CUI tenant details for org ID: {}", orgId);
        
        try {
            return cuiIntegrationServiceClient
                    .get()
                    .uri("/api/v1/cui/tenant-control-enabled?orgId={orgId}", orgId)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (Exception e) {
            log.error("Error getting CUI tenant details for org ID: {}", orgId, e);
            throw new RuntimeException("Failed to get CUI tenant details: " + e.getMessage());
        }
    }
    
    public Object checkTenantControlEnabled(Map<String, Object> arguments) {
        Long orgId = getLongArgument(arguments, "org_id");
        
        log.info("Checking tenant control enabled for org ID: {}", orgId);
        
        try {
            Object tenantDetails = cuiIntegrationServiceClient
                    .get()
                    .uri("/api/v1/cui/tenant-control-enabled?orgId={orgId}", orgId)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            
            return Map.of(
                "orgId", orgId,
                "tenantControlEnabled", tenantDetails != null,
                "details", tenantDetails
            );
        } catch (Exception e) {
            log.error("Error checking tenant control for org ID: {}", orgId, e);
            return Map.of(
                "orgId", orgId,
                "tenantControlEnabled", false,
                "error", e.getMessage()
            );
        }
    }
    
    public String setPasswordPolicy(Map<String, Object> arguments) {
        Long orgId = getLongArgument(arguments, "org_id");
        Boolean pciComplianceEnabled = (Boolean) arguments.get("pci_compliance_enabled");
        
        log.info("Setting password policy for org ID: {}, PCI compliance: {}", orgId, pciComplianceEnabled);
        
        try {
            Map<String, Object> requestBody = Map.of("pciComplianceEnabled", pciComplianceEnabled);
            
            cuiIntegrationServiceClient
                    .patch()
                    .uri("/api/v1/cui/organizations/{orgId}/set-password-policy", orgId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return "Password policy updated successfully for organization: " + orgId;
        } catch (Exception e) {
            log.error("Error setting password policy for org ID: {}", orgId, e);
            throw new RuntimeException("Failed to set password policy: " + e.getMessage());
        }
    }
    
    public OrganizationDto getTenantMappingStatus(Map<String, Object> arguments) {
        Long orgId = getLongArgument(arguments, "org_id");
        
        log.info("Getting tenant mapping status for org ID: {}", orgId);
        
        String sql = """
            SELECT ctm.org_id, ctm.cui_tenant_id, ctm.cui_org_id, ctm.cui_cluster_url,
                   ctm.status, ctm.created_at, ctm.updated_at
            FROM te_admin.tb_organization_cui_tenant_mapping ctm
            WHERE ctm.org_id = :orgId
            ORDER BY ctm.created_at DESC
            LIMIT 1
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("orgId", orgId);
        
        OrganizationDto organization = jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> 
            OrganizationDto.builder()
                    .orgId(rs.getLong("org_id"))
                    .cuiTenantId(rs.getString("cui_tenant_id"))
                    .cuiOrgId(rs.getString("cui_org_id"))
                    .cuiClusterUrl(rs.getString("cui_cluster_url"))
                    .mappingStatus(rs.getString("status"))
                    .mappingCreateTime(rs.getLong("created_at"))
                    .build()
        );
        enrichWithFeatureFlag(organization);
        return organization;
    }
    
    private void enrichWithFeatureFlag(OrganizationDto organization) {
        try {
            String sql = """
                SELECT * FROM te_admin.tb_organization_feature_flags 
                WHERE feature_id = 1073 AND org_id = :orgId
            """;
            
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("orgId", organization.getOrgId());
            
            jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {
                // Feature flag exists for this organization
                log.info("Feature flag 1073 found for org: {}", organization.getOrgId());
                organization.setPhase2FeatureFlagEnabled(true);
                return null;
            });
        } catch (Exception e) {
            log.debug("No feature flag 1073 found for org: {}", organization.getOrgId());
        }
    }
    
    
    private Long getLongArgument(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required argument '" + key + "' is missing");
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }
}
