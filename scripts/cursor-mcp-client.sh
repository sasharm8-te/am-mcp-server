#!/bin/bash

# Cursor MCP Client Script for Account Management MCP Server
# This script acts as a bridge between Cursor and the HTTP-based MCP server

set -e

MCP_SERVER_URL=${MCP_SERVER_URL:-"http://localhost:6080/mcp"}

# Function to log debug messages (to stderr so it doesn't interfere with stdout)
debug_log() {
    echo "[DEBUG] $1" >&2
}

# Function to handle MCP protocol over HTTP
handle_mcp_request() {
    local request_data="$1"
    
    debug_log "Received request: $request_data"
    
    # Extract method from JSON request
    local method
    method=$(echo "$request_data" | jq -r '.method // "unknown"')
    
    debug_log "Extracted method: $method"
    
    local response
    case "$method" in
        "initialize")
            response=$(curl -s -X POST "$MCP_SERVER_URL/initialize" \
                -H "Content-Type: application/json" \
                -d "$request_data")
            ;;
        "tools/list")
            response=$(curl -s -X POST "$MCP_SERVER_URL/tools/list" \
                -H "Content-Type: application/json" \
                -d "$request_data")
            ;;
        "tools/call")
            response=$(curl -s -X POST "$MCP_SERVER_URL/tools/call" \
                -H "Content-Type: application/json" \
                -d "$request_data")
            ;;
        "ping")
            response=$(curl -s -X POST "$MCP_SERVER_URL/ping" \
                -H "Content-Type: application/json" \
                -d "$request_data")
            ;;
        *)
            # Default fallback
            local request_id
            request_id=$(echo "$request_data" | jq -r '.id // null')
            response='{"jsonrpc": "2.0", "id": '"$request_id"', "error": {"code": -32601, "message": "Method not found: '"$method"'"}}'
            ;;
    esac
    
    debug_log "Response: $response"
    echo "$response"
}

# Main execution - Handle stdio communication
if [[ $# -eq 0 ]]; then
    debug_log "Starting MCP stdio mode"
    
    # Read line by line from stdin (MCP protocol over stdio)
    while IFS= read -r line; do
        if [[ -n "$line" ]]; then
            debug_log "Processing line: $line"
            handle_mcp_request "$line"
        fi
    done
else
    # Handle command line arguments for testing
    case "$1" in
        "--health")
            curl -s "$MCP_SERVER_URL/health"
            ;;
        "--test")
            echo '{"jsonrpc": "2.0", "id": "test", "method": "tools/list", "params": {}}' | handle_mcp_request
            ;;
        "--test-init")
            echo '{"jsonrpc": "2.0", "id": "1", "method": "initialize", "params": {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "cursor", "version": "1.0"}}}' | handle_mcp_request "$line"
            ;;
        *)
            echo "Usage: $0 [--health|--test|--test-init]"
            echo "Or run without arguments for stdio mode"
            exit 1
            ;;
    esac
fi
