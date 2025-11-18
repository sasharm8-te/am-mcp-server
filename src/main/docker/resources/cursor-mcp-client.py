#!/usr/bin/env python3

import json
import sys
import requests
import os
import logging
from typing import Dict, Any

# Set up logging to stderr so it doesn't interfere with stdout
# Use INFO level by default, DEBUG can be enabled with environment variable
log_level = logging.DEBUG if os.getenv('MCP_DEBUG', '').lower() == 'true' else logging.INFO
logging.basicConfig(
    level=log_level,
    format='[MCP-CLIENT] %(levelname)s: %(message)s',
    stream=sys.stderr
)
logger = logging.getLogger(__name__)

class CursorMCPClient:
    def __init__(self, server_url: str = "http://localhost:6080/mcp"):
        self.server_url = server_url.rstrip('/')
        logger.info(f"Initializing MCP client with server URL: {self.server_url}")
        
    def handle_request(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """Handle MCP request and forward to HTTP server"""
        method = request.get('method', '')
        request_id = request.get('id')
        
        logger.debug(f"Handling request: method={method}, id={request_id}")
        
        try:
            if method == 'initialize':
                logger.info("Handling initialize request")
                response = requests.post(
                    f"{self.server_url}/initialize",
                    json=request,
                    headers={'Content-Type': 'application/json'},
                    timeout=30
                )
            elif method == 'tools/list':
                logger.info("Handling tools/list request")
                response = requests.post(
                    f"{self.server_url}/tools/list",
                    json=request,
                    headers={'Content-Type': 'application/json'},
                    timeout=30
                )
            elif method == 'tools/call':
                tool_name = request.get('params', {}).get('name', 'unknown')
                logger.info(f"Handling tools/call request for tool: {tool_name}")
                response = requests.post(
                    f"{self.server_url}/tools/call",
                    json=request,
                    headers={'Content-Type': 'application/json'},
                    timeout=60  # Longer timeout for tool execution
                )
            elif method == 'ping':
                logger.info("Handling ping request")
                response = requests.post(
                    f"{self.server_url}/ping",
                    json=request,
                    headers={'Content-Type': 'application/json'},
                    timeout=10
                )
            else:
                logger.warning(f"Unknown method: {method}")
                return {
                    "jsonrpc": "2.0",
                    "id": request_id,
                    "error": {
                        "code": -32601,
                        "message": f"Method not found: {method}"
                    }
                }
            
            response.raise_for_status()
            result = response.json()
            logger.debug(f"Successfully handled request, response: {result}")
            return result
            
        except requests.exceptions.RequestException as e:
            logger.error(f"HTTP request failed: {str(e)}")
            return {
                "jsonrpc": "2.0",
                "id": request_id,
                "error": {
                    "code": -32603,
                    "message": f"Internal error: {str(e)}"
                }
            }
        except Exception as e:
            logger.error(f"Unexpected error: {str(e)}")
            return {
                "jsonrpc": "2.0",
                "id": request_id,
                "error": {
                    "code": -32603,
                    "message": f"Internal error: {str(e)}"
                }
            }
    
    def run(self):
        """Main loop to handle stdin/stdout communication"""
        logger.info("Starting MCP client stdio mode")
        
        try:
            for line in sys.stdin:
                line = line.strip()
                if not line:
                    continue
                
                logger.debug(f"Received line: {line}")
                    
                try:
                    request = json.loads(line)
                    response = self.handle_request(request)
                    response_json = json.dumps(response)
                    logger.debug(f"Sending response: {response_json}")
                    print(response_json, flush=True)
                except json.JSONDecodeError as e:
                    logger.error(f"JSON decode error: {str(e)}")
                    error_response = {
                        "jsonrpc": "2.0",
                        "id": None,
                        "error": {
                            "code": -32700,
                            "message": f"Parse error: {str(e)}"
                        }
                    }
                    print(json.dumps(error_response), flush=True)
                    
        except KeyboardInterrupt:
            logger.info("Received keyboard interrupt, shutting down")
        except Exception as e:
            logger.error(f"Unexpected error in main loop: {str(e)}")
            error_response = {
                "jsonrpc": "2.0",
                "id": None,
                "error": {
                    "code": -32603,
                    "message": f"Internal error: {str(e)}"
                }
            }
            print(json.dumps(error_response), flush=True)

if __name__ == "__main__":
    server_url = os.getenv('MCP_SERVER_URL', 'http://localhost:6080/mcp')
    
    # Handle command line arguments for testing
    if len(sys.argv) > 1:
        if sys.argv[1] == '--test':
            # Test mode
            test_request = {"jsonrpc": "2.0", "id": "test", "method": "tools/list", "params": {}}
            client = CursorMCPClient(server_url)
            response = client.handle_request(test_request)
            print(json.dumps(response, indent=2))
            sys.exit(0)
        elif sys.argv[1] == '--test-init':
            # Test initialize
            test_request = {
                "jsonrpc": "2.0", 
                "id": "1", 
                "method": "initialize", 
                "params": {
                    "protocolVersion": "2024-11-05", 
                    "capabilities": {}, 
                    "clientInfo": {"name": "cursor", "version": "1.0"}
                }
            }
            client = CursorMCPClient(server_url)
            response = client.handle_request(test_request)
            print(json.dumps(response, indent=2))
            sys.exit(0)
    
    # Normal stdio mode
    client = CursorMCPClient(server_url)
    client.run()
