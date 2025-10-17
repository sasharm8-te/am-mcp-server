# CUI Integration MCP Server

A Model Context Protocol (MCP) server that provides standardized access to the CUI Integration Service functionality. This server enables AI assistants and other tools to interact with user management, organization operations, synchronization monitoring, and system diagnostics through a unified interface.

## 🚀 Features

### 25+ MCP Tools Available

#### 👥 User Management
- `get_user_by_id` - Retrieve user details by UID or email
- `get_user_organizations` - Get all organizations a user belongs to
- `sync_user_profile` - Synchronize user profile information
- `create_user_in_tenant` - Create user in CUI tenant
- `sync_user_tenants` - Sync user across all their tenants
- `get_user_cui_metadata` - Retrieve CUI-specific user metadata

#### 🏢 Organization Management
- `get_organization_details` - Retrieve organization information
- `get_cui_tenant_details` - Get CUI tenant configuration
- `check_tenant_control_enabled` - Verify CUI tenant control status
- `set_password_policy` - Configure organization password policies
- `get_tenant_mapping_status` - Check tenant mapping sync status

#### 🔄 Synchronization Management
- `get_sync_retry_status` - Monitor failed synchronization attempts
- `trigger_user_sync_retry` - Manually retry failed user syncs
- `trigger_org_sync_retry` - Manually retry failed organization syncs
- `get_sync_metrics` - Retrieve synchronization performance metrics
- `clear_retry_queue` - Clear specific retry entries

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

- Java 21+
- Docker & Docker Compose
- Access to CUI Integration Service database

### 1. Using Docker Compose (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd cui-integration-mcp-server

# Set environment variables
export MCP_API_KEY="your-api-key"
export CUI_SERVICE_URL="http://your-cui-service:8080"

# Build and start services
docker-compose up --build

# The MCP server will be available at http://localhost:8080
```

### 2. Local Development

```bash
# Build the application
./gradlew build

# Run with local profile
SPRING_PROFILES_ACTIVE=local \
DB_URL=jdbc:mysql://localhost:3306/cui_integration \
DB_USERNAME=cui_user \
DB_PASSWORD=cui_password \
CUI_SERVICE_URL=http://localhost:8080 \
java -jar build/libs/cui-mcp-server.jar
```

### 3. Docker Build Only

```bash
# Build the JAR
./gradlew bootJar

# Build Docker image
docker build -t cui-mcp-server:latest .

# Run container
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/cui_integration \
  -e DB_USERNAME=cui_user \
  -e DB_PASSWORD=cui_password \
  -e CUI_SERVICE_URL=http://host.docker.internal:8080 \
  cui-mcp-server:latest
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` |
| `DB_URL` | Database connection URL | `jdbc:mysql://localhost:3306/cui_integration` |
| `DB_USERNAME` | Database username | `cui_user` |
| `DB_PASSWORD` | Database password | `cui_password` |
| `CUI_SERVICE_URL` | CUI Integration Service URL | `http://localhost:8080` |
| `MCP_API_KEY` | API key for authentication | (optional) |
| `ALLOWED_ORIGINS` | CORS allowed origins | `*` |

### Application Configuration

The server uses `application.yml` for configuration. Key sections:

```yaml
mcp:
  server:
    name: "CUI Integration MCP Server"
    tools:
      enabled: true
      timeout: 30000

database:
  url: ${DB_URL}
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}

external-services:
  cui-integration-service:
    base-url: ${CUI_SERVICE_URL}
```

## 🤖 MCP Client Configuration

### Claude Desktop

Add to your Claude Desktop configuration:

```json
{
  "mcpServers": {
    "cui-integration": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--network", "host",
        "-e", "MCP_API_KEY=your-api-key",
        "-e", "CUI_SERVICE_URL=http://localhost:8080",
        "-e", "DB_URL=jdbc:mysql://localhost:3306/cui_integration",
        "-e", "DB_USERNAME=cui_user",
        "-e", "DB_PASSWORD=cui_password",
        "cui-mcp-server:latest"
      ]
    }
  }
}
```

### Cline/Continue

```json
{
  "mcp": {
    "servers": {
      "cui-integration": {
        "transport": "stdio",
        "command": "docker",
        "args": [
          "run", "--rm", "-i",
          "--network", "host",
          "cui-mcp-server:latest"
        ],
        "env": {
          "CUI_SERVICE_URL": "http://localhost:8080",
          "DB_URL": "jdbc:mysql://localhost:3306/cui_integration"
        }
      }
    }
  }
}
```

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

## 🚀 Deployment

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

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

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
