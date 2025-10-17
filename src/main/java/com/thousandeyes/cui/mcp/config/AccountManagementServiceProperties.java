package com.thousandeyes.cui.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Account Management Service.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "external-services.account-management")
public class AccountManagementServiceProperties {
    
    private String grpcEndpoint;
    private boolean grpcUseTls = true;
    
    /**
     * Get gRPC endpoint (hostname without port for ingress).
     */
    public String getGrpcEndpoint() {
        return grpcEndpoint != null ? grpcEndpoint : "account-management-service.int-svc.eks1.stg.sfo2.1keyes.net";
    }
    
    /**
     * Whether to use TLS for gRPC connection.
     */
    public boolean isGrpcUseTls() {
        return grpcUseTls;
    }
}
