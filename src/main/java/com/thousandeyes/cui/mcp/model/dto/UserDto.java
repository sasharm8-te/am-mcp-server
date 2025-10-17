package com.thousandeyes.cui.mcp.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data transfer object for user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    
    private Long uid;
    private String name;
    private String email;
    private Boolean isAdmin;
    private Boolean hasLoginSsoPermission;
    private String locale;
    private Boolean flagRegistered;
    
    // CUI-specific metadata
    private String cuiUserId;
    private String cuiOrgId;
    private String cuiTenantId;
    
    // Organization information
    private List<OrganizationDto> organizations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationDto {
        private Long orgId;
        private String orgName;
        private String cuiTenantId;
        private String cuiOrgId;
        private Boolean cuiTenantControlEnabled;
    }
}
