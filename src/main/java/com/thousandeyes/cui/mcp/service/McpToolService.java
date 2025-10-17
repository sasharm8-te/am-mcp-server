package com.thousandeyes.cui.mcp.service;

import com.thousandeyes.cui.mcp.model.mcp.McpTool;
import com.thousandeyes.cui.mcp.model.mcp.McpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for orchestrating MCP tool operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpToolService {
    
    private final UserService userService;
    private final OrganizationService organizationService;
    private final SyncService syncService;
    private final MonitoringService monitoringService;
    private final AccountManagementGrpcService accountManagementService;
    
    /**
     * Get all available MCP tools.
     */
    public List<McpTool> getAvailableTools() {
        return List.of(
            // User Management Tools
            createUserTool("get_user_by_id", "Retrieve user details by UID or email"),
            createUserTool("get_user_organizations", "Get all organizations a user belongs to"),
            // createUserTool("sync_user_profile", "Synchronize user profile information"),
            // createUserTool("create_user_in_tenant", "Create user in CUI tenant"),
            // createUserTool("sync_user_tenants", "Sync user across all their tenants"),
            createUserTool("get_user_cui_metadata", "Retrieve CUI-specific user metadata"),
            
            // Organization Management Tools
            createOrgTool("get_organization_details", "Retrieve organization information"),
            createOrgTool("get_cui_tenant_details", "Get CUI tenant configuration for organization"),
            createOrgTool("check_tenant_control_enabled", "Verify if CUI tenant control is active"),
            // createOrgTool("set_password_policy", "Configure organization password policies"),
            createOrgTool("get_tenant_mapping_status", "Check tenant mapping synchronization status"),
            
            // Sync Management Tools
            createSyncTool("get_sync_retry_status", "Monitor failed synchronization attempts"),
            // createSyncTool("trigger_user_sync_retry", "Manually retry failed user synchronizations"),
            // createSyncTool("trigger_org_sync_retry", "Manually retry failed organization synchronizations"),
            createSyncTool("get_sync_metrics", "Retrieve synchronization performance metrics"),
            // createSyncTool("clear_retry_queue", "Clear specific retry entries"),
            

            //Account Management Tools
            createAccountTool("get_user_regions", "Get all regions a user belongs to"),

            // Monitoring Tools
            createMonitoringTool("get_service_health", "Check service health and dependencies"),
            createMonitoringTool("get_sync_statistics", "Retrieve synchronization statistics and trends"),
            createMonitoringTool("get_kafka_stream_status", "Monitor Kafka streams health"),
            createMonitoringTool("get_database_connectivity", "Check database connection status"),
            createMonitoringTool("get_external_service_status", "Verify external service connectivity")
        );
    }
    
    /**
     * Execute an MCP tool with the given arguments.
     */
    public McpResponse.ToolResult executeTool(String toolName, Map<String, Object> arguments) {
        try {
            log.info("Executing tool: {} with arguments: {}", toolName, arguments);
            
            // Map generic 'identifier' parameter to specific parameter names expected by service methods
            Map<String, Object> mappedArguments = mapArgumentsForTool(toolName, arguments);
            
            Object result = switch (toolName) {
                // User Management Tools
                case "get_user_by_id" -> userService.getUserById(mappedArguments);
                case "get_user_organizations" -> userService.getUserOrganizations(mappedArguments);
                // case "sync_user_profile" -> userService.syncUserProfile(mappedArguments);
                // case "create_user_in_tenant" -> userService.createUserInTenant(mappedArguments);
                // case "sync_user_tenants" -> userService.syncUserTenants(mappedArguments);
                case "get_user_cui_metadata" -> userService.getUserCuiMetadata(mappedArguments);
                
                // Organization Management Tools
                case "get_organization_details" -> organizationService.getOrganizationDetails(mappedArguments);
                case "get_cui_tenant_details" -> organizationService.getCuiTenantDetails(mappedArguments);
                case "check_tenant_control_enabled" -> organizationService.checkTenantControlEnabled(mappedArguments);
                // case "set_password_policy" -> organizationService.setPasswordPolicy(mappedArguments);
                case "get_tenant_mapping_status" -> organizationService.getTenantMappingStatus(mappedArguments);
                
                // Sync Management Tools
                case "get_sync_retry_status" -> syncService.getSyncRetryStatus(mappedArguments);
                // case "trigger_user_sync_retry" -> syncService.triggerUserSyncRetry(mappedArguments);
                // case "trigger_org_sync_retry" -> syncService.triggerOrgSyncRetry(mappedArguments);
                case "get_sync_metrics" -> syncService.getSyncMetrics(mappedArguments);
                // case "clear_retry_queue" -> syncService.clearRetryQueue(mappedArguments);
                
                // Account Management Tools
                case "get_user_regions" -> accountManagementService.getUserRegions(mappedArguments);
                
                // Monitoring Tools
                case "get_service_health" -> monitoringService.getServiceHealth(mappedArguments);
                case "get_sync_statistics" -> monitoringService.getSyncStatistics(mappedArguments);
                case "get_kafka_stream_status" -> monitoringService.getKafkaStreamStatus(mappedArguments);
                case "get_database_connectivity" -> monitoringService.getDatabaseConnectivity(mappedArguments);
                case "get_external_service_status" -> monitoringService.getExternalServiceStatus(mappedArguments);
                
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };
            
            return McpResponse.ToolResult.builder()
                    .content(List.of(McpResponse.ToolResult.Content.builder()
                            .type("text")
                            .text(formatResult(result))
                            .build()))
                    .isError(false)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error executing tool: {}", toolName, e);
            return McpResponse.ToolResult.builder()
                    .content(List.of(McpResponse.ToolResult.Content.builder()
                            .type("text")
                            .text("Error: " + e.getMessage())
                            .build()))
                    .isError(true)
                    .build();
        }
    }
    
    /**
     * Map generic 'identifier' parameter to specific parameter names expected by service methods.
     */
    private Map<String, Object> mapArgumentsForTool(String toolName, Map<String, Object> arguments) {
        log.info("Mapping arguments for tool: {} with original arguments: {}", toolName, arguments);
        Map<String, Object> mappedArguments = new java.util.HashMap<>(arguments);
        
        if (arguments.containsKey("identifier")) {
            Object identifier = arguments.get("identifier");
            
            switch (toolName) {
                // Organization Management Tools - map to org_id
                case "get_organization_details", "get_cui_tenant_details", "check_tenant_control_enabled", 
                     "set_password_policy", "get_tenant_mapping_status" -> {
                    mappedArguments.put("org_id", identifier);
                    mappedArguments.remove("identifier");
                }
                
                // User Management Tools that need uid parameter
                case "sync_user_profile", "create_user_in_tenant", "sync_user_tenants", "get_user_cui_metadata" -> {
                    mappedArguments.put("uid", identifier);
                    mappedArguments.remove("identifier");
                }
                
                // User Management Tools that need user_email parameter  
                case "get_user_organizations" -> {
                    mappedArguments.put("user_email", identifier);
                    mappedArguments.remove("identifier");
                }
                
                // Account Management Tools that need email parameter
                case "get_user_regions" -> {
                    mappedArguments.put("email", identifier);
                    mappedArguments.remove("identifier");
                }
                
                // Other user tools (get_user_by_id) keep identifier as is
                default -> {
                    // No mapping needed - services already handle "identifier" parameter
                }
            }
        }
        
        log.info("Final mapped arguments for tool {}: {}", toolName, mappedArguments);
        return mappedArguments;
    }
    
    private String formatResult(Object result) {
        if (result == null) {
            return "Operation completed successfully";
        }
        // Convert result to JSON string for display
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);
        } catch (Exception e) {
            return result.toString();
        }
    }
    
    private McpTool createUserTool(String name, String description) {
        return McpTool.builder()
                .name(name)
                .description(description)
                .inputSchema(createInputSchema(name))
                .build();
    }
    
    private McpTool createOrgTool(String name, String description) {
        return McpTool.builder()
                .name(name)
                .description(description)
                .inputSchema(createInputSchema(name))
                .build();
    }
    
    private McpTool createSyncTool(String name, String description) {
        return McpTool.builder()
                .name(name)
                .description(description)
                .inputSchema(createInputSchema(name))
                .build();
    }
    
    private McpTool createMonitoringTool(String name, String description) {
        return McpTool.builder()
                .name(name)
                .description(description)
                .inputSchema(createInputSchema(name))
                .build();
    }
    
    private McpTool createAccountTool(String name, String description) {
        return McpTool.builder()
                .name(name)
                .description(description)
                .inputSchema(createInputSchema(name))
                .build();
    }
    
    private McpTool.InputSchema createInputSchema(String toolName) {
        return switch (toolName) {
            // User Management Tools
            case "get_user_by_id" -> createUserByIdSchema();
            case "get_user_organizations" -> createUserOrganizationsSchema();
            case "sync_user_profile" -> createSyncUserProfileSchema();
            case "create_user_in_tenant" -> createUserInTenantSchema();
            case "sync_user_tenants" -> createSyncUserTenantsSchema();
            case "get_user_cui_metadata" -> createUserCuiMetadataSchema();
            
            // Organization Management Tools
            case "get_organization_details" -> createOrgDetailsSchema();
            case "get_cui_tenant_details" -> createCuiTenantDetailsSchema();
            case "check_tenant_control_enabled" -> createTenantControlSchema();
            case "set_password_policy" -> createPasswordPolicySchema();
            case "get_tenant_mapping_status" -> createTenantMappingSchema();
            
            // Account Management Tools
            case "get_user_regions" -> createUserRegionsSchema();
            
            // Default generic schema for other tools
            default -> createGenericSchema();
        };
    }
    
    private McpTool.InputSchema createUserByIdSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("User ID (numeric) or email address")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createUserOrganizationsSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("User's email address")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createSyncUserProfileSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("User ID")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createUserInTenantSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("User ID")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createSyncUserTenantsSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("User ID")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createUserCuiMetadataSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("User ID")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createOrgDetailsSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("Organization ID")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createCuiTenantDetailsSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("Organization ID")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createTenantControlSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("Organization ID")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createPasswordPolicySchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("Organization ID")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createTenantMappingSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("Organization ID")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
    
    private McpTool.InputSchema createUserRegionsSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("User's email address")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of("identifier"))
                .build();
    }
    
    private McpTool.InputSchema createGenericSchema() {
        Map<String, McpTool.InputSchema.Property> properties = Map.of(
            "identifier", McpTool.InputSchema.Property.builder()
                    .type("string")
                    .description("Identifier parameter")
                    .build()
        );
        return McpTool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(List.of())
                .build();
    }
}
