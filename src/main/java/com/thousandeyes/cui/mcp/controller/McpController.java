package com.thousandeyes.cui.mcp.controller;

import com.thousandeyes.cui.mcp.model.mcp.McpRequest;
import com.thousandeyes.cui.mcp.model.mcp.McpResponse;
import com.thousandeyes.cui.mcp.model.mcp.McpTool;
import com.thousandeyes.cui.mcp.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for handling MCP protocol requests.
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {
    
    private final McpToolService mcpToolService;
    
    /**
     * Handle MCP initialization request.
     */
    @PostMapping("/initialize")
    public ResponseEntity<McpResponse> initialize(@RequestBody McpRequest request) {
        log.info("MCP initialization request received");
        
        Map<String, Object> result = Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of("listChanged", true)
            ),
            "serverInfo", Map.of(
                "name", "CUI Integration MCP Server",
                "version", "1.0.0"
            )
        );
        
        return ResponseEntity.ok(McpResponse.success(request.getId(), result));
    }
    
    /**
     * List available MCP tools.
     */
    @PostMapping("/tools/list")
    public ResponseEntity<McpResponse> listTools(@RequestBody McpRequest request) {
        log.info("MCP tools list request received");
        
        try {
            List<McpTool> tools = mcpToolService.getAvailableTools();
            Map<String, Object> result = Map.of("tools", tools);
            
            return ResponseEntity.ok(McpResponse.success(request.getId(), result));
        } catch (Exception e) {
            log.error("Error listing tools", e);
            return ResponseEntity.ok(McpResponse.error(request.getId(), -1, "Failed to list tools: " + e.getMessage()));
        }
    }
    
    /**
     * Execute an MCP tool.
     */
    @PostMapping("/tools/call")
    public ResponseEntity<McpResponse> callTool(@RequestBody McpRequest request) {
        log.info("MCP tool call request received: {}", request.getParams().getName());
        
        try {
            String toolName = request.getParams().getName();
            Map<String, Object> arguments = request.getParams().getArguments();
            
            McpResponse.ToolResult result = mcpToolService.executeTool(toolName, arguments);
            
            return ResponseEntity.ok(McpResponse.success(request.getId(), result));
        } catch (Exception e) {
            log.error("Error executing tool", e);
            return ResponseEntity.ok(McpResponse.error(request.getId(), -1, "Tool execution failed: " + e.getMessage()));
        }
    }
    
    /**
     * Handle ping requests for connection testing.
     */
    @PostMapping("/ping")
    public ResponseEntity<McpResponse> ping(@RequestBody McpRequest request) {
        log.debug("MCP ping request received");
        return ResponseEntity.ok(McpResponse.success(request.getId(), Map.of()));
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "CUI Integration MCP Server",
            "version", "1.0.0",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
