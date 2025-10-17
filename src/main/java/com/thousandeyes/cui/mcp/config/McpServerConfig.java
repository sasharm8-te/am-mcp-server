package com.thousandeyes.cui.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the MCP server.
 */
@Data
@Component
@ConfigurationProperties(prefix = "mcp.server")
public class McpServerConfig {
    
    private String name = "CUI Integration MCP Server";
    private String version = "1.0.0";
    private int port = 8080;
    private String transport = "stdio";
    
    private Tools tools = new Tools();
    
    @Data
    public static class Tools {
        private boolean enabled = true;
        private long timeout = 30000;
        private RateLimit rateLimit = new RateLimit();
        
        @Data
        public static class RateLimit {
            private int requestsPerMinute = 100;
            private int burstSize = 10;
        }
    }
}
