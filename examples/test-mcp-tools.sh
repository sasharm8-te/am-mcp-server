#!/bin/bash

# Test script for MCP tools
# This script demonstrates how to test the MCP server tools

set -e

BASE_URL="http://localhost:6080/mcp"

echo "Testing CUI Integration MCP Server..."
echo "Base URL: $BASE_URL"
echo ""

# Test 1: Health check
echo "1. Testing health check..."
curl -s "$BASE_URL/health" | jq '.'
echo ""

# Test 2: List available tools
echo "2. Listing available tools..."
curl -s -X POST "$BASE_URL/tools/list" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/list",
    "params": {}
  }' | jq '.result.tools[] | {name: .name, description: .description}'
echo ""

# Test 3: Get service health
echo "3. Testing get_service_health tool..."
curl -s -X POST "$BASE_URL/tools/call" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "2",
    "method": "tools/call",
    "params": {
      "name": "get_service_health",
      "arguments": {}
    }
  }' | jq '.result.content[0].text | fromjson'
echo ""

# Test 4: Get user by ID (example)
echo "4. Testing get_user_by_id tool..."
curl -s -X POST "$BASE_URL/tools/call" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "3",
    "method": "tools/call",
    "params": {
      "name": "get_user_by_id",
      "arguments": {
        "identifier": "john.doe@example.com",
        "include_cui_metadata": true
      }
    }
  }' | jq '.result'
echo ""

# Test 5: Get sync statistics
echo "5. Testing get_sync_statistics tool..."
curl -s -X POST "$BASE_URL/tools/call" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "4",
    "method": "tools/call",
    "params": {
      "name": "get_sync_statistics",
      "arguments": {
        "time_range": "24h"
      }
    }
  }' | jq '.result.content[0].text | fromjson'
echo ""

echo "MCP Server testing completed!"
