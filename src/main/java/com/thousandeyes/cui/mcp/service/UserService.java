package com.thousandeyes.cui.mcp.service;

import com.thousandeyes.cui.mcp.model.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for user-related MCP operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final WebClient cuiIntegrationServiceClient;
    
    public UserDto getUserById(Map<String, Object> arguments) {
        String identifier = (String) arguments.get("identifier");
        Boolean includeCuiMetadata = (Boolean) arguments.getOrDefault("include_cui_metadata", true);
        
        log.info("Getting user by identifier: {}", identifier);
        
        // Try to parse as UID first, then treat as email
        try {
            Long uid = Long.parseLong(identifier);
            return getUserByUid(uid, includeCuiMetadata);
        } catch (NumberFormatException e) {
            return getUserByEmail(identifier, includeCuiMetadata);
        }
    }
    
    public List<UserDto.OrganizationDto> getUserOrganizations(Map<String, Object> arguments) {
        // Support both 'user_email' and 'identifier' parameter names for flexibility
        String userEmail = (String) arguments.get("user_email");
        if (userEmail == null) {
            userEmail = (String) arguments.get("identifier");
        }
        
        if (userEmail == null) {
            throw new IllegalArgumentException("Either 'user_email' or 'identifier' parameter is required");
        }
        
        log.info("Getting organizations for user: {}", userEmail);
        
        String sql = """
            SELECT DISTINCT o.org_id, o.organization_name, octm.cui_tenant_id, octm.cui_org_id
            FROM te_admin.tb_users u
            JOIN te_admin.tb_users_accounts ua ON u.uid = ua.uid
            JOIN te_admin.tb_accounts a ON ua.aid = a.aid
            JOIN te_admin.tb_organizations o ON a.org_id = o.org_id
            LEFT JOIN te_admin.tb_organization_cui_tenant_mapping octm ON o.org_id = octm.org_id AND octm.status = 'SUCCESS'
            WHERE u.email = :email
            AND u.delete_time IS NULL 
            AND a.delete_time IS NULL
            AND o.delete_time IS NULL
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", userEmail);
        
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> 
            UserDto.OrganizationDto.builder()
                    .orgId(rs.getLong("org_id"))
                    .orgName(rs.getString("organization_name"))
                    .cuiTenantId(rs.getString("cui_tenant_id"))
                    .cuiOrgId(rs.getString("cui_org_id"))
                    .build()
        );
    }
    
    public String syncUserProfile(Map<String, Object> arguments) {
        Long uid = getLongArgument(arguments, "uid");
        
        log.info("Syncing user profile for UID: {}", uid);
        
        try {
            // Call the CUI Integration Service API
            String response = cuiIntegrationServiceClient
                    .patch()
                    .uri("/api/v1/users/{uid}", uid)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return "User profile sync initiated successfully for UID: " + uid;
        } catch (Exception e) {
            log.error("Error syncing user profile for UID: {}", uid, e);
            throw new RuntimeException("Failed to sync user profile: " + e.getMessage());
        }
    }
    
    public Object createUserInTenant(Map<String, Object> arguments) {
        Long uid = getLongArgument(arguments, "uid");
        Long aid = getLongArgument(arguments, "aid");
        
        log.info("Creating user in tenant - UID: {}, AID: {}", uid, aid);
        
        try {
            // Call the CUI Integration Service API
            return cuiIntegrationServiceClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/users")
                            .queryParamIfPresent("uid", Optional.ofNullable(uid))
                            .queryParam("aid", aid)
                            .build())
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (Exception e) {
            log.error("Error creating user in tenant - UID: {}, AID: {}", uid, aid, e);
            throw new RuntimeException("Failed to create user in tenant: " + e.getMessage());
        }
    }
    
    public String syncUserTenants(Map<String, Object> arguments) {
        Long uid = getLongArgument(arguments, "uid");
        
        log.info("Syncing user tenants for UID: {}", uid);
        
        try {
            // Call the CUI Integration Service API
            cuiIntegrationServiceClient
                    .patch()
                    .uri("/api/v1/users/{uid}/sync-tenants", uid)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return "User tenant sync initiated successfully for UID: " + uid;
        } catch (Exception e) {
            log.error("Error syncing user tenants for UID: {}", uid, e);
            throw new RuntimeException("Failed to sync user tenants: " + e.getMessage());
        }
    }
    
    public UserDto getUserCuiMetadata(Map<String, Object> arguments) {
        log.info("getUserCuiMetadata called with arguments: {}", arguments);
        Long uid = getLongArgument(arguments, "uid");
        if(uid == null) {
            log.info("uid is null, getting uid from identifier");
            uid = getLongArgument(arguments, "identifier");
        }
        log.info("Getting CUI metadata for UID: {}", uid);
        
        String sql = """
            SELECT u.uid, u.name, u.email, NULL as cui_user_id, NULL as cui_org_id
            FROM te_admin.tb_users u
            WHERE u.uid = :uid AND u.delete_time IS NULL
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("uid", uid);
        
        UserDto user = jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> 
            UserDto.builder()
                    .uid(rs.getLong("uid"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .cuiUserId(rs.getString("cui_user_id"))
                    .cuiOrgId(rs.getString("cui_org_id"))
                    .build()
        );
        if (user != null) {
            // Add CUI metadata
            enrichWithCuiMetadata(user);
        }
        return user;
    }
    
    private UserDto getUserByUid(Long uid, Boolean includeCuiMetadata) {
        String sql = """
            SELECT u.uid, u.name, u.email, u.flag_registered
            FROM te_admin.tb_users u
            WHERE u.uid = :uid AND u.delete_time IS NULL
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("uid", uid);
        
        UserDto user = jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> 
            UserDto.builder()
                    .uid(rs.getLong("uid"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .flagRegistered(rs.getBoolean("flag_registered"))
                    .build()
        );
        
        if (includeCuiMetadata && user != null) {
            // Add CUI metadata
            enrichWithCuiMetadata(user);
        }
        
        return user;
    }
    
    private UserDto getUserByEmail(String email, Boolean includeCuiMetadata) {
        String sql = """
            SELECT u.uid, u.name, u.email, u.flag_registered
            FROM te_admin.tb_users u
            WHERE u.email = :email AND u.delete_time IS NULL
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", email);
        
        UserDto user = jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> 
            UserDto.builder()
                    .uid(rs.getLong("uid"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .flagRegistered(rs.getBoolean("flag_registered"))
                    .build()
        );
        
        if (includeCuiMetadata && user != null) {
            enrichWithCuiMetadata(user);
        }
        
        return user;
    }
    
    private void enrichWithCuiMetadata(UserDto user) {
        log.info("Enriching user with CUI metadata: {}", user);
        try {
            String sql = """
                SELECT value
                FROM te_admin.tb_users_metadata
                WHERE uid = :uid AND property = 'cuiUserMetadata'
            """;
            
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("uid", user.getUid());
            
            String cuiMetadata = jdbcTemplate.queryForObject(sql, params, String.class);
            log.info("CUI metadata: {}", cuiMetadata);
            
            // Parse CUI metadata JSON and set fields
            if (cuiMetadata != null && !cuiMetadata.trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(cuiMetadata);
                    
                    String cuiUserId = jsonNode.has("cuiUserId") ? jsonNode.get("cuiUserId").asText() : null;
                    String cuiOrgId = jsonNode.has("cuiOrgId") ? jsonNode.get("cuiOrgId").asText() : null;
                    
                    user.setCuiUserId(cuiUserId);
                    user.setCuiOrgId(cuiOrgId);
                    
                    log.info("Parsed CUI metadata - UserId: {}, OrgId: {}", cuiUserId, cuiOrgId);
                } catch (Exception jsonEx) {
                    log.error("Error parsing CUI metadata JSON: {}", jsonEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error enriching user with CUI metadata: {}", e);
        }
    }
    
    private Long getLongArgument(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        log.info("getLongArgument: key='{}', value='{}', valueType='{}'", key, value, value != null ? value.getClass().getSimpleName() : "null");
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }
}
