package com.thousandeyes.cui.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main application class for the CUI Integration MCP Server.
 * 
 * This server provides Model Context Protocol (MCP) tools for interacting
 * with the CUI Integration Service, enabling AI assistants and other tools
 * to manage users, organizations, and synchronization operations.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
