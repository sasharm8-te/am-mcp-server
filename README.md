# Account Management MCP Server

A Model Context Protocol (MCP) server that provides standardized access to Account Management Service functionality. This server enables AI assistants and other tools to interact with user management, organization operations, synchronization monitoring, and system diagnostics through a unified interface.

## 🚀 Features

### 25+ MCP Tools Available

#### 👥 User Management
- `get_user_by_id` - Retrieve user details by UID or email
- `get_user_organizations` - Get all organizations a user belongs to
- `sync_user_profile` - Synchronize user profile information (Not implemented)
- `create_user_in_tenant` - Create user in CUI tenant (Not implemented)
- `sync_user_tenants` - Sync user across all their tenants (Not implemented)
- `get_user_cui_metadata` - Retrieve CUI-specific user metadata

#### 🏢 Organization Management
- `get_organization_details` - Retrieve organization information
- `get_cui_tenant_details` - Get CUI tenant configuration
- `check_tenant_control_enabled` - Verify CUI tenant control status
- `set_password_policy` - Configure organization password policies (Not implemented)
- `get_tenant_mapping_status` - Check tenant mapping sync status

#### 🔄 Synchronization Management
- `get_sync_retry_status` - Monitor failed synchronization attempts
- `trigger_user_sync_retry` - Manually retry failed user syncs (Not implemented)
- `trigger_org_sync_retry` - Manually retry failed organization syncs (Not implemented)
- `get_sync_metrics` - Retrieve synchronization performance metrics
- `clear_retry_queue` - Clear specific retry entries (Not implemented)

#### 📊 Monitoring & Diagnostics
- `get_service_health` - Check service health and dependencies
- `get_sync_statistics` - Retrieve synchronization statistics
- `get_kafka_stream_status` - Monitor Kafka streams health
- `get_database_connectivity` - Check database connection status
- `get_external_service_status` - Verify external service connectivity

## 🏗️ Architecture

```
CUI Integration MCP Server
├── MCP Protocol Handler
├── Tool Orchestration Layer
├── Business Logic Services
│   ├── User Service
│   ├── Organization Service
│   ├── Sync Service
│   └── Monitoring Service
├── Data Access Layer
│   ├── Direct Database Access
│   └── External API Clients
└── Configuration & Security
```

> 📖 **Detailed Flow Documentation**: For comprehensive information about request/response flows, sequence diagrams, and component interactions, see [Flow Documentation](docs/flow-documentation.md)

## 🛠️ Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- MCP-compatible client (Cursor, Claude Desktop, etc.)

### 1. 🐳 Using Docker (Recommended for MCP Clients)

**Step 1: Build the Docker image**

```bash
# Build JAR and Docker image
./gradlew buildImage

# Tag the image
IMAGE_ID=$(docker images -q | head -1)
docker tag $IMAGE_ID accounting/am-mcp-server:latest
docker tag $IMAGE_ID accounting/am-mcp-server:1.0.0

# Verify
docker images | grep am-mcp-server
```

**Step 2: Run the docker**
```
# Start the server as a persistent container
docker run -d \
  --name am-mcp-server \
  -p 6080:6080 \
  -e SPRING_PROFILES_ACTIVE=local \
  accounting/am-mcp-server:latest

# Wait a few seconds for startup, then try again
---
```

**Step 3: Configure your MCP client**

Add to your MCP configuration file:
- **Cursor**: `~/.cursor/mcp.json`
- **Claude Desktop**: `~/Library/Application Support/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "am-mcp": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--network=host",
        "-e",
        "SPRING_PROFILES_ACTIVE=local",
        "accounting/am-mcp-server:latest",
        "--stdio"
      ],
      "env": {
        "MCP_SERVER_URL": "http://localhost:6080/mcp"
      }
    }
  }
}
```

**Why `--network=host` instead of `-p 6080:6080`?**
- STDIO mode requires the container to connect back to localhost
- `--network=host` allows the container to access your local database
- The container starts/stops automatically with each MCP request
- For persistent HTTP server mode, use `-p 6080:6080` instead (see Docker HTTP Mode section)

**Step 3: Restart your MCP client**

The `am-mcp` tools will now be available! The Docker container will:
- Start automatically when your MCP client connects
- Run the Spring Boot server in the background
- Connect via STDIO mode for seamless integration
- Stop when your MCP client disconnects

### 2. 🔨 Local Development

```bash
# Build the application
./gradlew build

# Run with local profile
SPRING_PROFILES_ACTIVE=local \
java -jar build/libs/am-mcp-server.jar
```

Then configure your MCP client to use the local Python wrapper:

```json
{
  "mcpServers": {
    "am-mcp": {
      "command": "python3",
      "args": ["/path/to/am-mcp-server/scripts/cursor-mcp-client.py"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:6080/mcp"
      }
    }
  }
}
```

### 3. 🚀 Docker HTTP Mode (For persistent server or direct API access)

```bash
# Run as a standalone HTTP server
docker run -d \
  --name am-mcp-server \
  -p 6080:6080 \
  -e SPRING_PROFILES_ACTIVE=local \
  accounting/am-mcp-server:latest

# The MCP server will be available at http://localhost:6080/mcp

# Test it
curl http://localhost:6080/actuator/health

# Or use Docker Compose
docker-compose up -d
```

**Note:** In HTTP mode, the container runs continuously. For MCP clients (Cursor/Claude), use the Docker STDIO mode shown in the Quick Start section instead.

---

## 📖 Detailed Docker Setup

For comprehensive Docker setup instructions, troubleshooting, and advanced configurations, see:

**[📘 Docker Setup Guide](docker-setup.md)**

This guide includes:
- teDocker deployment for ThousandEyes internal use
- Docker registry configuration
- STDIO vs HTTP mode details
- Complete troubleshooting section
- CI/CD integration examples

---

## 🧪 Testing the MCP Server

### Test with Docker (Recommended)

```bash
# Using the built image with STDIO mode
docker run --rm -i --network=host \
  -e SPRING_PROFILES_ACTIVE=local \
  accounting/am-mcp-server:latest \
  --stdio

# Send a test request (type this and press Enter)
{"jsonrpc":"2.0","id":1,"method":"tools/list"}

# Or test HTTP mode
docker run -d --name am-mcp-test \
  -p 6080:6080 \
  -e SPRING_PROFILES_ACTIVE=local \
  accounting/am-mcp-server:latest

# Test the health endpoint
curl http://localhost:6080/actuator/health

# Test an MCP tool
curl -X POST http://localhost:6080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

# Clean up
docker stop am-mcp-test && docker rm am-mcp-test
```

### Test with MCP Client

Once configured in Cursor or Claude Desktop, try these commands:
- "Get organization details for org ID 1"
- "Get user details for user ID 12345"
- "Check service health"

## 🔧 Configuration

### Application Configuration

The server uses `application.yml` for configuration.

## 🤖 MCP Client Configuration

### Cursor

Add to `~/.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "am-mcp": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--network=host",
        "-e",
        "SPRING_PROFILES_ACTIVE=local",
        "accounting/am-mcp-server:latest",
        "--stdio"
      ],
      "env": {
        "MCP_SERVER_URL": "http://localhost:6080/mcp"
      }
    }
  }
}
```

### Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "am-mcp": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--network=host",
        "-e",
        "SPRING_PROFILES_ACTIVE=local",
        "accounting/am-mcp-server:latest",
        "--stdio"
      ],
      "env": {
        "MCP_SERVER_URL": "http://localhost:6080/mcp"
      }
    }
  }
}
```

**Note:** After updating the configuration, restart your MCP client for changes to take effect.

### Alternative: Local Development Mode

For local development without Docker:

```json
{
  "mcpServers": {
    "am-mcp": {
      "command": "python3",
      "args": ["/path/to/am-mcp-server/scripts/cursor-mcp-client.py"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:6080/mcp"
      }
    }
  }
}
```

Make sure the Spring Boot server is running separately with `./gradlew bootRun`.

## 📚 Documentation

### 📖 Flow Documentation
- **[Request/Response Flow](docs/flow-documentation.md)** - Comprehensive guide to MCP server flows
  - MCP server registration process
  - Tool execution sequence diagrams
  - Component interaction patterns
  - Error handling strategies
  - Parameter mapping details

## 📚 Usage Examples

### Get User Information

```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "tools/call",
  "params": {
    "name": "get_user_by_id",
    "arguments": {
      "identifier": "john.doe@example.com",
      "include_cui_metadata": true
    }
  }
}
```

### Check Organization Tenant Status

```json
{
  "jsonrpc": "2.0",
  "id": "2", 
  "method": "tools/call",
  "params": {
    "name": "get_cui_tenant_details",
    "arguments": {
      "org_id": 12345
    }
  }
}
```

### Monitor Sync Status

```json
{
  "jsonrpc": "2.0",
  "id": "3",
  "method": "tools/call", 
  "params": {
    "name": "get_sync_retry_status",
    "arguments": {
      "page": 0,
      "size": 10,
      "entity_type": "USER"
    }
  }
}
```

## 🔍 Monitoring

### Health Checks

- **Application Health**: `GET /actuator/health`
- **MCP Health**: `GET /mcp/health`
- **Metrics**: `GET /actuator/metrics`
- **Prometheus**: `GET /actuator/prometheus`

### Logging

Logs are structured and include:
- Request/response details for each MCP tool
- Database query performance
- External service call metrics
- Error details with correlation IDs

## 🛡️ Security

### Authentication
- Optional API key authentication via `MCP_API_KEY`
- CORS configuration for web clients
- Secure credential management

### Container Security
- Non-root user execution
- Minimal base image with security updates
- Health checks and resource limits

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### Manual Testing
```bash
# Start the server
docker-compose up

# Test MCP protocol
curl -X POST http://localhost:8080/mcp/tools/list \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "id": "1", "method": "tools/list", "params": {}}'
```

### Production Deployment

1. **Build the application**:
   ```bash
   ./gradlew bootJar
   docker build -t cui-mcp-server:1.0.0 .
   ```

2. **Deploy with Docker**:
   ```bash
   docker run -d \
     --name cui-mcp-server \
     -p 8080:8080 \
     -e SPRING_PROFILES_ACTIVE=production \
     -e DB_URL=jdbc:mysql://prod-db:3306/cui_integration \
     -e DB_USERNAME=cui_user \
     -e DB_PASSWORD=secure_password \
     -e CUI_SERVICE_URL=https://cui-service.prod.com \
     cui-mcp-server:1.0.0
   ```

3. **Kubernetes Deployment**:
   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: cui-mcp-server
   spec:
     replicas: 2
     selector:
       matchLabels:
         app: cui-mcp-server
     template:
       metadata:
         labels:
           app: cui-mcp-server
       spec:
         containers:
         - name: cui-mcp-server
           image: cui-mcp-server:1.0.0
           ports:
           - containerPort: 8080
           env:
           - name: SPRING_PROFILES_ACTIVE
             value: "production"
           - name: DB_URL
             valueFrom:
               secretKeyRef:
                 name: cui-mcp-secrets
                 key: db-url
   ```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-tool`
3. Make your changes and add tests
4. Commit your changes: `git commit -am 'Add new MCP tool'`
5. Push to the branch: `git push origin feature/new-tool`
6. Submit a pull request

## 🆘 Support

- **Documentation**: See the [design document](.cursor/contexts/mcp-server-design.md)
- **Issues**: Create an issue in the repository
- **Contact**: ThousandEyes CUI Team

## 🔄 Changelog

### v1.0.0 (Initial Release)
- 25+ MCP tools for CUI Integration Service
- Docker containerization
- Comprehensive monitoring and health checks
- Production-ready configuration
- Full MCP protocol compliance
