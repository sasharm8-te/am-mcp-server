package com.thousandeyes.cui.mcp.model.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents an MCP tool definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpTool {
    
    private String name;
    private String description;
    private InputSchema inputSchema;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputSchema {
        @Builder.Default
        private String type = "object";
        private Map<String, Property> properties;
        private List<String> required;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Property {
            private String type;
            private String description;
            private Object defaultValue;
            private List<String> enumValues;
        }
    }
}
