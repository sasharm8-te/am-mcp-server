# CUI Integration MCP Server Design Document

## Overview

The CUI Integration MCP (Model Context Protocol) Server provides a standardized interface for AI assistants and other tools to interact with the CUI Integration Service. This server exposes the core functionality of the CUI Integration Service through MCP tools, enabling seamless integration with AI workflows.

## Architecture

### Core Components

```
cui-mcp-server/
├── src/main/java/com/thousandeyes/cui/mcp/
│   ├── McpServerApplication.java           # Main Spring Boot application
│   ├── config/                            # Configuration classes
│   │   ├── McpServerConfig.java           # MCP server configuration
│   │   ├── DatabaseConfig.java            # Database connection setup
│   │   └── ExternalServiceConfig.java     # External service clients
│   ├── controller/
│   │   └── McpController.java             # MCP protocol endpoint
│   ├── tools/                             # MCP tool implementations
│   │   ├── user/
│   │   │   ├── UserManagementTools.java   # User CRUD operations
│   │   │   └── UserSyncTools.java         # User synchronization tools
│   │   ├── organization/
│   │   │   ├── OrganizationTools.java     # Organization management
│   │   │   └── TenantManagementTools.java # CUI tenant operations
│   │   ├── sync/
│   │   │   ├── SyncRetryTools.java        # Retry management
│   │   │   └── SyncMonitoringTools.java   # Sync status monitoring
│   │   ├── monitoring/
│   │   │   ├── HealthCheckTools.java      # Service health checks
│   │   │   └── MetricsTools.java          # Performance metrics
│   │   └── config/
│   │       └── ConfigurationTools.java   # Configuration management
│   ├── service/                           # Business logic services
│   │   ├── McpToolService.java            # MCP tool orchestration
│   │   ├── UserService.java               # User operations
│   │   ├── OrganizationService.java       # Organization operations
│   │   ├── SyncService.java               # Synchronization logic
│   │   └── MonitoringService.java         # Monitoring and metrics
│   ├── model/                             # Data models
│   │   ├── mcp/
│   │   │   ├── McpRequest.java            # MCP request structure
│   │   │   ├── McpResponse.java           # MCP response structure
│   │   │   ├── McpTool.java               # Tool definition
│   │   │   └── McpError.java              # Error handling
│   │   └── dto/
│   │       ├── UserDto.java               # User data transfer object
│   │       ├── OrganizationDto.java       # Organization DTO
│   │       ├── SyncStatusDto.java         # Sync status DTO
│   │       └── HealthDto.java             # Health check DTO
│   ├── client/                            # External service clients
│   │   ├── CuiIntegrationServiceClient.java # Main service client
│   │   ├── DatabaseClient.java            # Direct DB access
│   │   └── ExternalServiceClient.java     # Other external services
│   └── exception/                         # Exception handling
│       ├── McpException.java              # Base MCP exception
│       ├── ToolNotFoundException.java     # Tool not found
│       └── ServiceUnavailableException.java # Service errors
├── src/main/resources/
│   ├── application.yml                    # Main configuration
│   ├── mcp-tools-config.yml              # Tool definitions
│   └── logback.xml                        # Logging configuration
├── docker/
│   ├── Dockerfile                         # Container definition
│   └── docker-compose.yml                # Local development setup
├── build.gradle                          # Build configuration
└── README.md                             # Documentation
```

## MCP Tools Catalog

### 1. User Management Tools

#### `get_user_by_id`
- **Description**: Retrieve user details by UID or email
- **Parameters**:
  - `identifier` (string, required): User ID (numeric) or email address
  - `include_cui_metadata` (boolean, optional): Include CUI-specific metadata
- **Returns**: User object with profile information and CUI metadata

#### `get_user_organizations`
- **Description**: Get all organizations a user belongs to
- **Parameters**:
  - `user_email` (string, required): User's email address
- **Returns**: List of organization details with CUI tenant information

#### `sync_user_profile`
- **Description**: Synchronize user profile information to CUI
- **Parameters**:
  - `uid` (integer, required): User ID
- **Returns**: Sync operation status

#### `create_user_in_tenant`
- **Description**: Create user in specific CUI tenant
- **Parameters**:
  - `uid` (integer, optional): User ID (null for SP-initiated SAML JIT)
  - `aid` (integer, required): Account group ID
- **Returns**: Tenant information and CUI user ID

#### `sync_user_tenants`
- **Description**: Sync user across all their tenants
- **Parameters**:
  - `uid` (integer, required): User ID
- **Returns**: Sync operation results for each tenant

#### `get_user_cui_metadata`
- **Description**: Retrieve CUI-specific user metadata
- **Parameters**:
  - `uid` (integer, required): User ID
- **Returns**: CUI user ID, organization ID, and tenant mappings

### 2. Organization & Tenant Management Tools

#### `get_organization_details`
- **Description**: Retrieve organization information
- **Parameters**:
  - `org_id` (integer, required): Organization ID
- **Returns**: Organization details and CUI migration status

#### `get_cui_tenant_details`
- **Description**: Get CUI tenant configuration for organization
- **Parameters**:
  - `org_id` (integer, required): Organization ID
- **Returns**: CUI tenant ID, organization ID, cluster URL, and control status

#### `check_tenant_control_enabled`
- **Description**: Verify if CUI tenant control is active
- **Parameters**:
  - `org_id` (integer, required): Organization ID
- **Returns**: Boolean status and integration stage information

#### `set_password_policy`
- **Description**: Configure organization password policies
- **Parameters**:
  - `org_id` (integer, required): Organization ID
  - `pci_compliance_enabled` (boolean, required): Enable PCI compliance
- **Returns**: Policy update status

#### `get_tenant_mapping_status`
- **Description**: Check tenant mapping synchronization status
- **Parameters**:
  - `org_id` (integer, required): Organization ID
- **Returns**: Mapping status, creation date, and sync details

### 3. Synchronization & Retry Management Tools

#### `get_sync_retry_status`
- **Description**: Monitor failed synchronization attempts
- **Parameters**:
  - `page` (integer, optional): Page number for pagination
  - `size` (integer, optional): Page size (default: 20)
  - `entity_type` (string, optional): Filter by entity type (USER, ORGANIZATION)
- **Returns**: Paginated list of retry records with failure details

#### `trigger_user_sync_retry`
- **Description**: Manually retry failed user synchronizations
- **Parameters**:
  - `uid` (integer, required): User ID
  - `org_id` (integer, optional): Specific organization ID
- **Returns**: Retry operation status

#### `trigger_org_sync_retry`
- **Description**: Manually retry failed organization synchronizations
- **Parameters**:
  - `org_id` (integer, required): Organization ID
  - `sync_type` (string, optional): Type of sync (TENANT_MIRROR, SSO_CONFIG)
- **Returns**: Retry operation status

#### `get_sync_metrics`
- **Description**: Retrieve synchronization performance metrics
- **Parameters**:
  - `time_range` (string, optional): Time range (1h, 24h, 7d, 30d)
  - `metric_type` (string, optional): Specific metric type
- **Returns**: Aggregated sync metrics and performance data

#### `clear_retry_queue`
- **Description**: Clear specific retry entries
- **Parameters**:
  - `entity_id` (string, required): Entity identifier
  - `entity_type` (string, required): Entity type (USER, ORGANIZATION)
- **Returns**: Cleanup operation status

### 4. Monitoring & Diagnostics Tools

#### `get_service_health`
- **Description**: Check service health and dependencies
- **Parameters**: None
- **Returns**: Overall health status and dependency checks

#### `get_sync_statistics`
- **Description**: Retrieve synchronization statistics and trends
- **Parameters**:
  - `time_range` (string, optional): Time range for statistics
- **Returns**: Success rates, failure counts, and trend analysis

#### `get_kafka_stream_status`
- **Description**: Monitor Kafka streams health
- **Parameters**: None
- **Returns**: Stream status, lag information, and processing metrics

#### `get_database_connectivity`
- **Description**: Check database connection status
- **Parameters**: None
- **Returns**: Connection pool status and query performance

#### `get_external_service_status`
- **Description**: Verify external service connectivity
- **Parameters**:
  - `service_name` (string, optional): Specific service (IDP_PROXY, AMS)
- **Returns**: Service availability and response times

### 5. Configuration Management Tools

#### `get_cui_configuration`
- **Description**: Retrieve CUI integration settings
- **Parameters**: None
- **Returns**: Current configuration values and feature flags

#### `validate_tenant_configuration`
- **Description**: Validate tenant setup for organization
- **Parameters**:
  - `org_id` (integer, required): Organization ID
- **Returns**: Validation results and configuration issues

#### `get_supported_organizations`
- **Description**: List organizations with CUI integration
- **Parameters**:
  - `status` (string, optional): Filter by status (ACTIVE, PENDING, FAILED)
- **Returns**: List of organizations with integration status

#### `get_feature_flags`
- **Description**: Retrieve current feature flag settings
- **Parameters**: None
- **Returns**: Feature flag states and descriptions

## Configuration

### Application Configuration (`application.yml`)

```yaml
mcp:
  server:
    name: "CUI Integration MCP Server"
    version: "1.0.0"
    port: 8080
    transport: "stdio"
  
  tools:
    enabled: true
    timeout: 30000
    rate-limit:
      requests-per-minute: 100
      burst-size: 10

database:
  url: jdbc:mysql://localhost:3306/cui_integration
  username: ${DB_USERNAME:cui_user}
  password: ${DB_PASSWORD:cui_password}
  driver-class-name: com.mysql.cj.jdbc.Driver
  connection-pool:
    maximum-pool-size: 10
    minimum-idle: 2

external-services:
  cui-integration-service:
    base-url: ${CUI_SERVICE_URL:http://localhost:8080}
    timeout: 30000
    retry:
      max-attempts: 3
      backoff-delay: 1000
  
  idp-proxy:
    base-url: ${IDP_PROXY_URL:http://localhost:8081}
    timeout: 15000
  
  account-management:
    base-url: ${AMS_URL:http://localhost:8082}
    timeout: 15000

security:
  api-key: ${MCP_API_KEY:}
  allowed-origins: ${ALLOWED_ORIGINS:*}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### Tool Configuration (`mcp-tools-config.yml`)

```yaml
tools:
  user-management:
    - name: "get_user_by_id"
      description: "Retrieve user details by UID or email"
      category: "user"
      parameters:
        - name: "identifier"
          type: "string"
          required: true
          description: "User ID (numeric) or email address"
        - name: "include_cui_metadata"
          type: "boolean"
          required: false
          default: true
          description: "Include CUI-specific metadata"
    
    - name: "get_user_organizations"
      description: "Get all organizations a user belongs to"
      category: "user"
      parameters:
        - name: "user_email"
          type: "string"
          required: true
          description: "User's email address"

  organization-management:
    - name: "get_cui_tenant_details"
      description: "Get CUI tenant configuration for organization"
      category: "organization"
      parameters:
        - name: "org_id"
          type: "integer"
          required: true
          description: "Organization ID"
    
    - name: "check_tenant_control_enabled"
      description: "Verify if CUI tenant control is active"
      category: "organization"
      parameters:
        - name: "org_id"
          type: "integer"
          required: true
          description: "Organization ID"

  sync-management:
    - name: "get_sync_retry_status"
      description: "Monitor failed synchronization attempts"
      category: "sync"
      parameters:
        - name: "page"
          type: "integer"
          required: false
          default: 0
          description: "Page number for pagination"
        - name: "size"
          type: "integer"
          required: false
          default: 20
          description: "Page size"

  monitoring:
    - name: "get_service_health"
      description: "Check service health and dependencies"
      category: "monitoring"
      parameters: []
```

## Docker Deployment

### Dockerfile

```dockerfile
FROM openjdk:21-jdk-slim

LABEL maintainer="ThousandEyes CUI Team"
LABEL description="CUI Integration MCP Server"

WORKDIR /app

# Copy application JAR
COPY build/libs/cui-mcp-server-*.jar app.jar

# Create non-root user
RUN groupadd -r mcpuser && useradd -r -g mcpuser mcpuser
RUN chown -R mcpuser:mcpuser /app
USER mcpuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=docker

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  cui-mcp-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_USERNAME=cui_user
      - DB_PASSWORD=cui_password
      - CUI_SERVICE_URL=http://cui-integration-service:8080
      - MCP_API_KEY=${MCP_API_KEY}
    depends_on:
      - mysql
    networks:
      - cui-network
    restart: unless-stopped
    
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: cui_integration
      MYSQL_USER: cui_user
      MYSQL_PASSWORD: cui_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - cui-network
    restart: unless-stopped

volumes:
  mysql_data:

networks:
  cui-network:
    driver: bridge
```

## MCP Client Configuration

### Claude Desktop Configuration

```json
{
  "mcpServers": {
    "cui-integration": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--network", "host",
        "-e", "MCP_API_KEY=your-api-key",
        "cui-mcp-server:latest"
      ],
      "env": {
        "CUI_SERVICE_URL": "http://localhost:8080",
        "DB_USERNAME": "cui_user",
        "DB_PASSWORD": "cui_password"
      }
    }
  }
}
```

### Cline Configuration

```json
{
  "mcp": {
    "servers": {
      "cui-integration": {
        "transport": "stdio",
        "command": "docker",
        "args": [
          "run", "--rm", "-i",
          "cui-mcp-server:latest"
        ],
        "env": {
          "CUI_SERVICE_URL": "http://localhost:8080"
        }
      }
    }
  }
}
```

## Implementation Phases

### Phase 1: Core Infrastructure (Week 1-2)
- [ ] Set up Spring Boot project structure
- [ ] Implement MCP protocol handling
- [ ] Create basic tool framework
- [ ] Add database connectivity
- [ ] Implement 5 core user management tools

### Phase 2: Extended Functionality (Week 3-4)
- [ ] Add organization and tenant management tools
- [ ] Implement sync monitoring and retry tools
- [ ] Add comprehensive error handling
- [ ] Create Docker configuration
- [ ] Implement security features

### Phase 3: Production Readiness (Week 5-6)
- [ ] Add monitoring and health check tools
- [ ] Implement configuration management tools
- [ ] Add comprehensive testing
- [ ] Performance optimization
- [ ] Documentation and deployment guides

## Security Considerations

### Authentication & Authorization
- API key-based authentication for MCP clients
- Role-based access control for sensitive operations
- Secure credential management for external services

### Data Protection
- Encryption in transit for all external communications
- Secure handling of sensitive user data
- Audit logging for all operations

### Container Security
- Non-root user execution
- Minimal base image with security updates
- Resource limits and health checks

## Monitoring & Observability

### Metrics
- Request/response metrics for each MCP tool
- Performance metrics for database and external service calls
- Error rates and retry statistics

### Logging
- Structured logging with correlation IDs
- Audit logs for sensitive operations
- Performance and debugging logs

### Health Checks
- Liveness and readiness probes
- Dependency health checks
- Circuit breaker patterns for external services

## Development Guidelines

### Code Standards
- Follow Spring Boot best practices
- Use dependency injection and configuration management
- Implement comprehensive error handling
- Write unit and integration tests

### Testing Strategy
- Unit tests for all service classes
- Integration tests for MCP tool endpoints
- Contract tests for external service interactions
- Performance tests for critical paths

### Documentation
- JavaDoc for all public APIs
- README with setup and usage instructions
- API documentation for MCP tools
- Deployment and configuration guides
