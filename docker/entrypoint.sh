#!/bin/bash
set -e

MODE="${1:-http}"

case "$MODE" in
  --stdio|stdio)
    echo "[Entrypoint] Starting in STDIO mode..."
    echo "[Entrypoint] Will connect to database from application.yml config"
    
    # Start Spring Boot server in background
    java -jar /app/app.jar &
    SERVER_PID=$!
    
    # Wait for server to be ready
    echo "[Entrypoint] Waiting for server to start..."
    for i in {1..30}; do
      if curl -s http://localhost:6080/actuator/health > /dev/null 2>&1; then
        echo "[Entrypoint] Server ready, starting STDIO client..."
        break
      fi
      if [ $i -eq 30 ]; then
        echo "[Entrypoint] ERROR: Server failed to start within 30 seconds"
        kill $SERVER_PID 2>/dev/null || true
        exit 1
      fi
      sleep 1
    done
    
    # Run Python client for stdio communication
    export MCP_SERVER_URL="http://localhost:6080/mcp"
    python3 /app/scripts/cursor-mcp-client.py
    
    # Cleanup
    kill $SERVER_PID 2>/dev/null || true
    ;;
    
  --http|http|*)
    echo "[Entrypoint] Starting in HTTP mode..."
    echo "[Entrypoint] Will connect to database from application.yml config"
    exec java -jar /app/app.jar
    ;;
esac

