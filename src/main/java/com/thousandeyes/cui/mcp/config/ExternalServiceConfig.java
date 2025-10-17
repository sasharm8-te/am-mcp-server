package com.thousandeyes.cui.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Configuration for external service clients.
 */
@Configuration
@ConfigurationProperties(prefix = "external-services")
@Data
public class ExternalServiceConfig {
    
    private ServiceConfig cuiIntegrationService = new ServiceConfig();
    private ServiceConfig idpProxy = new ServiceConfig();
    private ServiceConfig accountManagement = new ServiceConfig();
    
    @Data
    public static class ServiceConfig {
        private String baseUrl;
        private long timeout = 30000;
        private Retry retry = new Retry();
        
        @Data
        public static class Retry {
            private int maxAttempts = 3;
            private long backoffDelay = 1000;
        }
    }
    
    @Bean("cuiIntegrationServiceClient")
    public WebClient cuiIntegrationServiceClient() {
        return WebClient.builder()
                .baseUrl(cuiIntegrationService.getBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
    
    @Bean("idpProxyClient")
    public WebClient idpProxyClient() {
        return WebClient.builder()
                .baseUrl(idpProxy.getBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
    
    @Bean("accountManagementClient")
    public WebClient accountManagementClient() {
        return WebClient.builder()
                .baseUrl(accountManagement.getBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
}
