package com.thousandeyes.cui.mcp.model.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents an MCP response to a client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {
    
    @JsonProperty("jsonrpc")
    @Builder.Default
    private String jsonrpc = "2.0";
    
    private String id;
    private Object result;
    private McpError error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpError {
        private int code;
        private String message;
        private Object data;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResult {
        private List<Content> content;
        private boolean isError;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Content {
            private String type;
            private String text;
        }
    }
    
    public static McpResponse success(String id, Object result) {
        return McpResponse.builder()
                .id(id)
                .result(result)
                .build();
    }
    
    public static McpResponse error(String id, int code, String message) {
        return McpResponse.builder()
                .id(id)
                .error(McpError.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }
}
