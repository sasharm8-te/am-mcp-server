#!/bin/bash

# CUI Integration MCP Server Startup Script
# This script starts the MCP server with proper configuration

set -e

# Default values
DEFAULT_PROFILE="local"
DEFAULT_PORT="8080"
DEFAULT_DB_URL="jdbc:mysql://localhost:3306/cui_integration"
DEFAULT_DB_USERNAME="cui_user"
DEFAULT_CUI_SERVICE_URL="http://localhost:8080"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            SPRING_PROFILES_ACTIVE="$2"
            shift 2
            ;;
        --port)
            SERVER_PORT="$2"
            shift 2
            ;;
        --db-url)
            DB_URL="$2"
            shift 2
            ;;
        --db-username)
            DB_USERNAME="$2"
            shift 2
            ;;
        --db-password)
            DB_PASSWORD="$2"
            shift 2
            ;;
        --cui-service-url)
            CUI_SERVICE_URL="$2"
            shift 2
            ;;
        --api-key)
            MCP_API_KEY="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --profile PROFILE           Spring profile (default: local)"
            echo "  --port PORT                 Server port (default: 8080)"
            echo "  --db-url URL                Database URL"
            echo "  --db-username USERNAME      Database username"
            echo "  --db-password PASSWORD      Database password"
            echo "  --cui-service-url URL       CUI Integration Service URL"
            echo "  --api-key KEY               MCP API key"
            echo "  --help                      Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Set defaults if not provided
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-$DEFAULT_PROFILE}
SERVER_PORT=${SERVER_PORT:-$DEFAULT_PORT}
DB_URL=${DB_URL:-$DEFAULT_DB_URL}
DB_USERNAME=${DB_USERNAME:-$DEFAULT_DB_USERNAME}
CUI_SERVICE_URL=${CUI_SERVICE_URL:-$DEFAULT_CUI_SERVICE_URL}

# Validate required parameters
if [[ -z "$DB_PASSWORD" ]]; then
    echo "Error: Database password is required. Use --db-password or set DB_PASSWORD environment variable."
    exit 1
fi

# Export environment variables
export SPRING_PROFILES_ACTIVE
export SERVER_PORT
export DB_URL
export DB_USERNAME
export DB_PASSWORD
export CUI_SERVICE_URL
export MCP_API_KEY

echo "Starting CUI Integration MCP Server..."
echo "Profile: $SPRING_PROFILES_ACTIVE"
echo "Port: $SERVER_PORT"
echo "Database: $DB_URL"
echo "CUI Service: $CUI_SERVICE_URL"
echo ""

# Find the JAR file
JAR_FILE="build/libs/cui-mcp-server.jar"
if [[ ! -f "$JAR_FILE" ]]; then
    echo "JAR file not found: $JAR_FILE"
    echo "Please build the project first: ./gradlew bootJar"
    exit 1
fi

# Start the server
java -jar "$JAR_FILE"
