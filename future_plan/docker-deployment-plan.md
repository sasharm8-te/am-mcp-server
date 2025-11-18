# 🚀 Docker Deployment Plan for AM-MCP Server

**Created:** October 17, 2025  
**Status:** Planning Phase  
**Priority:** Medium

---

## 📊 Current State Analysis

### ✅ What's Working

- Multi-stage Dockerfile with proper build process
- Docker Compose setup for local development
- Health checks configured
- Non-root user security
- Basic build script structure

### ❌ Issues Found

1. **Port Mismatch** - All Docker configs use `8080` but application changed to `6080`
2. **Java Version Mismatch** - `Dockerfile.simple` uses Java 11, but app requires Java 17
3. **MCP Protocol Issue** - Current setup is HTTP-based, but MCP clients (Cursor/Claude) need **stdio mode**
4. **Registry Placeholder** - `build-docker.sh` has `"your-registry.com"` placeholder
5. **No Client Wrapper** - Docker image doesn't include the Python client script
6. **Versioning** - No clear version tagging strategy
7. **Documentation Gap** - README doesn't explain stdio vs HTTP modes

---

## 📋 Deployment Plan Overview

### Phase 1: Fix Core Issues

1. Update all Docker configs to use port `6080`
2. Fix Dockerfile.simple to use Java 17
3. Add Python client script to Docker image for stdio support
4. Create a dedicated Docker entrypoint that supports both HTTP and stdio modes

### Phase 2: Build & Registry Setup

5. Update `build-docker.sh` with proper registry configuration
6. Add multi-architecture support (amd64/arm64)
7. Implement semantic versioning
8. Add GitHub Actions or CI/CD pipeline

### Phase 3: User-Facing Deployment

9. Create production-ready Dockerfile
10. Update all example configs for users
11. Publish to Docker Hub or private registry
12. Create user documentation

---

## 🏗️ Detailed Implementation Plan

### 1. Create New Dockerfile Structure

**Files to Create/Modify:**

```
am-mcp-server/
├── Dockerfile                          # Main production Dockerfile (MODIFY)
├── Dockerfile.simple                   # Quick build version (MODIFY)
├── Dockerfile.mcp-client              # NEW: Client-focused image with stdio
├── docker/
│   ├── entrypoint.sh                  # NEW: Smart entrypoint script
│   └── init-db.sql                    # (existing)
├── build-docker.sh                     # MODIFY: Enhanced build script
├── docker-compose.yml                  # MODIFY: Update ports
└── docker-compose.production.yml      # NEW: Production deployment
```

### 2. Dockerfile Changes Needed

#### Main Dockerfile Updates

```dockerfile
# Changes needed:
- EXPOSE 8080  →  EXPOSE 6080
- Update healthcheck port: 8080 → 6080
- Add Python3 and requests library for MCP client
- Copy scripts/cursor-mcp-client.py into image
- Add flexible entrypoint supporting both modes
```

#### Dockerfile.simple Updates

```dockerfile
# Changes needed:
- FROM openjdk:11-jre-slim  →  FROM openjdk:17-jre-slim
- EXPOSE 8080  →  EXPOSE 6080
- Update healthcheck port: 8080 → 6080
```

#### NEW: Dockerfile.mcp-client

```dockerfile
# Purpose: Lightweight image specifically for MCP clients
# Features:
- Includes Python client wrapper
- Defaults to stdio mode
- Minimal size for fast pulls
- Optimized for Cursor/Claude Desktop integration
```

### 3. Smart Entrypoint Script

**File:** `docker/entrypoint.sh`

```bash
#!/bin/bash
# Supports two modes:
# 1. HTTP mode (default): Runs Spring Boot server on port 6080
# 2. STDIO mode: Runs Python wrapper that talks to HTTP server
# 
# Usage:
#   docker run ... am-mcp-server:latest              # HTTP mode
#   docker run ... am-mcp-server:latest --stdio      # STDIO mode
#   docker run ... am-mcp-server:latest python3 /app/cursor-mcp-client.py  # Direct stdio

MODE="${1:-http}"

case "$MODE" in
  --stdio|stdio)
    # Start server in background and connect via stdio client
    java -jar /app/app.jar &
    SERVER_PID=$!
    sleep 5  # Wait for server to start
    export MCP_SERVER_URL="http://localhost:6080/mcp"
    python3 /app/scripts/cursor-mcp-client.py
    kill $SERVER_PID
    ;;
  --http|http|*)
    # Standard HTTP server mode
    exec java -jar /app/app.jar
    ;;
esac
```

### 4. Registry & Versioning Strategy

#### Registry Options

| Option | Pros | Cons | Recommendation |
|--------|------|------|----------------|
| **Docker Hub** | Public, easy access | Requires Docker Hub account | Good for OSS |
| **GitHub Container Registry** | Integrated with repo, free for public | Requires GitHub auth | **Recommended** |
| **Private Registry** | Full control, secure | Requires infrastructure | For enterprise |

#### Recommended Registry URL Structure

```bash
# GitHub Container Registry (Recommended)
ghcr.io/thousandeyes/am-mcp-server:latest
ghcr.io/thousandeyes/am-mcp-server:1.0.0
ghcr.io/thousandeyes/am-mcp-server:1.0.0-alpine

# Or Docker Hub
docker.io/thousandeyes/am-mcp-server:latest

# Or Private
your-registry.company.com/thousandeyes/am-mcp-server:latest
```

#### Versioning Scheme

```bash
# Semantic Versioning (Recommended)
am-mcp-server:1.0.0          # Specific version
am-mcp-server:1.0            # Minor version
am-mcp-server:1              # Major version
am-mcp-server:latest         # Latest stable

# Special Tags
am-mcp-server:main           # Development branch
am-mcp-server:pr-123         # Pull request builds
am-mcp-server:sha-abc123     # Specific commit
```

### 5. Updated Build Script Features

**File:** `build-docker.sh` (Enhanced Version)

```bash
#!/bin/bash
# Enhanced Docker Build Script for AM-MCP Server

# Features to Add:
# - Support for multiple registries
# - Automated version bumping
# - Multi-architecture builds (buildx)
# - Build-time argument passing
# - Automated testing before push
# - Changelog generation
# - Dry-run mode
# - Tag management

# Example usage:
# ./build-docker.sh --registry ghcr.io --push
# ./build-docker.sh --version 1.2.0 --multi-arch
# ./build-docker.sh --test-only
```

**Configuration Variables:**

```bash
REGISTRY="${REGISTRY:-ghcr.io/thousandeyes}"
IMAGE_NAME="am-mcp-server"
VERSION="${VERSION:-1.0.0}"
BUILD_PLATFORMS="linux/amd64,linux/arm64"
PUSH_TO_REGISTRY="${PUSH:-false}"
RUN_TESTS="${TEST:-true}"
```

### 6. User Connection Methods

#### Method A: Direct Docker Run (HTTP Mode)

Users run server separately, then connect via HTTP bridge.

**Cursor Configuration (~/.cursor/mcp.json):**

```json
{
  "mcpServers": {
    "am-mcp": {
      "command": "python3",
      "args": ["/path/to/cursor-mcp-client.py"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:6080/mcp"
      }
    }
  }
}
```

**Start Server:**

```bash
docker run -d -p 6080:6080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/cui_integration \
  -e DB_USERNAME=cui_user \
  -e DB_PASSWORD=secret \
  ghcr.io/thousandeyes/am-mcp-server:latest
```

**Pros:** Server runs independently, can be accessed by multiple clients  
**Cons:** Two-step setup

#### Method B: Combined Container (STDIO Mode)

Single container runs both server and client.

**Cursor Configuration:**

```json
{
  "mcpServers": {
    "am-mcp": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "-e", "DB_URL=jdbc:mysql://host.docker.internal:3306/cui_integration",
        "-e", "DB_USERNAME=cui_user",
        "-e", "DB_PASSWORD=secret",
        "ghcr.io/thousandeyes/am-mcp-server:latest",
        "--stdio"
      ]
    }
  }
}
```

**Pros:** Single-step setup, easier for users  
**Cons:** New container for each client connection

#### Method C: Docker Compose for Full Stack

**docker-compose.production.yml:**

```yaml
version: '3.8'

services:
  am-mcp-server:
    image: ghcr.io/thousandeyes/am-mcp-server:latest
    container_name: am-mcp-server
    ports:
      - "6080:6080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DATABASE_URL=${DATABASE_URL}
      - DATABASE_USERNAME=${DATABASE_USERNAME}
      - DATABASE_PASSWORD=${DATABASE_PASSWORD}
      - CUI_SERVICE_URL=${CUI_SERVICE_URL}
      - MCP_API_KEY=${MCP_API_KEY}
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:6080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

**Usage:**

```bash
# User downloads docker-compose.production.yml and .env
docker-compose -f docker-compose.production.yml up -d
```

**Pros:** Production-ready, easy to manage  
**Cons:** Requires docker-compose knowledge

#### **Recommendation:** Support All Three Methods

### 7. Configuration Management

#### Environment Variables to Support

```bash
# ===== Server Configuration =====
SERVER_PORT=6080
SPRING_PROFILES_ACTIVE=production

# ===== Database Configuration =====
DATABASE_URL=jdbc:mysql://db-host:3306/cui_integration
DATABASE_USERNAME=cui_user
DATABASE_PASSWORD=secure_password
DATABASE_DRIVER=com.mysql.cj.jdbc.Driver

# Connection Pool
DATABASE_POOL_MAX_SIZE=10
DATABASE_POOL_MIN_IDLE=2
DATABASE_CONNECTION_TIMEOUT=30000

# ===== External Services =====
# CUI Integration Service
CUI_SERVICE_URL=https://cui-integration-service.company.com
CUI_SERVICE_TIMEOUT=30000

# IDP Proxy
IDP_PROXY_URL=https://idp-proxy.company.com
IDP_PROXY_TIMEOUT=15000

# Account Management Service
AMS_GRPC_ENDPOINT=account-management.company.com:443
AMS_GRPC_USE_TLS=true

# ===== MCP Configuration =====
MCP_MODE=http                         # NEW: http|stdio
MCP_SERVER_URL=http://localhost:6080/mcp  # For stdio mode
MCP_API_KEY=optional-api-key
MCP_TOOLS_ENABLED=true
MCP_TIMEOUT=30000

# ===== Security =====
ALLOWED_ORIGINS=*
API_KEY_ENABLED=false

# ===== Monitoring =====
MANAGEMENT_ENDPOINTS_ENABLED=true
METRICS_EXPORT_PROMETHEUS=true

# ===== Logging =====
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_AM_MCP=DEBUG
```

#### Configuration File: `.env.example`

```bash
# Copy this file to .env and update values
# Then run: docker-compose --env-file .env up

# Database
DATABASE_URL=jdbc:mysql://localhost:3306/cui_integration
DATABASE_USERNAME=cui_user
DATABASE_PASSWORD=CHANGEME

# External Services
CUI_SERVICE_URL=https://cui-service.company.com
AMS_GRPC_ENDPOINT=ams-service.company.com:443

# Security
MCP_API_KEY=CHANGEME
```

### 8. Documentation Updates

#### Files to Create/Update

##### `README.md` (Update)

Add comprehensive Docker section:

```markdown
## 🐳 Docker Deployment

### Quick Start with Docker

Pull the latest image:
```bash
docker pull ghcr.io/thousandeyes/am-mcp-server:latest
```

Run the server:
```bash
docker run -p 6080:6080 \
  -e DATABASE_PASSWORD=secret \
  ghcr.io/thousandeyes/am-mcp-server:latest
```

### For MCP Clients (Cursor/Claude)

See [docs/deployment-guide.md](docs/deployment-guide.md) for detailed configuration.
```

##### `docs/deployment-guide.md` (NEW)

```markdown
# Deployment Guide

## Table of Contents
1. Docker Deployment Options
2. Configuration Reference
3. Production Deployment
4. Kubernetes Deployment
5. Monitoring & Troubleshooting
```

##### `examples/cursor-config-docker.json` (NEW)

```json
{
  "mcpServers": {
    "am-mcp": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "ghcr.io/thousandeyes/am-mcp-server:latest",
        "--stdio"
      ],
      "env": {
        "DATABASE_URL": "jdbc:mysql://host.docker.internal:3306/db",
        "DATABASE_USERNAME": "user",
        "DATABASE_PASSWORD": "pass"
      }
    }
  }
}
```

##### `examples/claude-desktop-config-docker.json` (UPDATE)

Update to use correct image and port.

##### `CHANGELOG.md` (NEW)

```markdown
# Changelog

## [1.0.0] - TBD

### Added
- Docker support with multi-mode deployment
- GitHub Container Registry publishing
- Production-ready docker-compose
- Comprehensive deployment documentation

### Changed
- Server port from 8080 to 6080
- Docker images now support both HTTP and stdio modes

### Fixed
- Java version compatibility in Dockerfile.simple
```

### 9. CI/CD Pipeline

#### GitHub Actions Workflow

**File:** `.github/workflows/docker-build-push.yml`

```yaml
name: Build and Push Docker Images

on:
  push:
    branches: [main, develop]
    tags: ['v*']
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew bootJar

      - name: Run tests
        run: ./gradlew test

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Log in to Container Registry
        if: github.event_name != 'pull_request'
        uses: docker/login-action@v2
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=semver,pattern={{major}}
            type=sha

      - name: Build and push Docker image
        uses: docker/build-push-action@v4
        with:
          context: .
          platforms: linux/amd64,linux/arm64
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Test Docker image
        run: |
          docker run --rm ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.meta.outputs.version }} \
            java -jar /app/app.jar --version || true
```

#### Additional CI/CD Workflows

**`.github/workflows/test.yml`** - Run tests on every PR  
**`.github/workflows/release.yml`** - Create releases and update changelog  
**`.github/workflows/security-scan.yml`** - Scan Docker images for vulnerabilities

### 10. Testing Strategy

#### Pre-Deployment Testing Checklist

```bash
# 1. Build locally
./gradlew clean bootJar
docker build -t am-mcp-server:test .

# 2. Test HTTP mode
docker run -d -p 6080:6080 --name test-http am-mcp-server:test
curl http://localhost:6080/actuator/health
curl -X POST http://localhost:6080/mcp/tools/list \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}'
docker stop test-http && docker rm test-http

# 3. Test STDIO mode
echo '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}' | \
  docker run -i --rm am-mcp-server:test --stdio

# 4. Test with Cursor
# Update ~/.cursor/mcp.json with test image
# Open Cursor and verify tools are available

# 5. Integration tests
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
docker-compose -f docker-compose.test.yml down

# 6. Security scan
docker scan am-mcp-server:test

# 7. Multi-architecture build test
docker buildx build --platform linux/amd64,linux/arm64 -t am-mcp-server:test .
```

#### Automated Testing

**File:** `tests/docker-integration-test.sh`

```bash
#!/bin/bash
# Automated Docker integration tests

set -e

IMAGE="${1:-am-mcp-server:latest}"

echo "Testing image: $IMAGE"

# Test 1: Health check
echo "Test 1: Health check..."
docker run -d -p 6080:6080 --name test-health $IMAGE
sleep 10
curl -f http://localhost:6080/actuator/health || exit 1
docker stop test-health && docker rm test-health
echo "✓ Health check passed"

# Test 2: MCP tools list
echo "Test 2: MCP tools list..."
docker run -d -p 6080:6080 --name test-tools $IMAGE
sleep 10
RESPONSE=$(curl -s -X POST http://localhost:6080/mcp/tools/list \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}')
echo $RESPONSE | grep -q "get_user_by_id" || exit 1
docker stop test-tools && docker rm test-tools
echo "✓ MCP tools list passed"

# Test 3: STDIO mode
echo "Test 3: STDIO mode..."
RESPONSE=$(echo '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}' | \
  docker run -i --rm $IMAGE --stdio)
echo $RESPONSE | grep -q "get_user_by_id" || exit 1
echo "✓ STDIO mode passed"

echo ""
echo "All tests passed! ✓"
```

---

## 🎯 Implementation Priority

### High Priority (Must Fix Before Release)

| Task | Description | Estimated Effort |
|------|-------------|------------------|
| ✅ Port Updates | Change 8080 → 6080 in all Docker files | 30 min |
| ✅ Java Version Fix | Update Dockerfile.simple to Java 17 | 15 min |
| ✅ Smart Entrypoint | Create entrypoint.sh for mode switching | 1 hour |
| ✅ Python Client Integration | Add cursor-mcp-client.py to Docker image | 30 min |

**Total: ~2.5 hours**

### Medium Priority (For User-Friendly Release)

| Task | Description | Estimated Effort |
|------|-------------|------------------|
| ✅ Registry Configuration | Update build-docker.sh with real registry | 30 min |
| ✅ Production Compose | Create docker-compose.production.yml | 1 hour |
| ✅ Example Configs | Update all example JSON configs | 45 min |
| ✅ README Update | Add comprehensive Docker section | 1 hour |
| ✅ Deployment Guide | Create docs/deployment-guide.md | 2 hours |

**Total: ~5 hours**

### Low Priority (Nice to Have)

| Task | Description | Estimated Effort |
|------|-------------|------------------|
| ⏳ Multi-arch Builds | Add linux/arm64 support | 2 hours |
| ⏳ CI/CD Pipeline | GitHub Actions for automated builds | 3 hours |
| ⏳ Automated Testing | Integration test suite | 3 hours |
| ⏳ Security Scanning | Vulnerability scanning in CI/CD | 1 hour |
| ⏳ Versioning Automation | Semantic release setup | 2 hours |
| ⏳ Health Dashboard | Monitoring UI | 8 hours |

**Total: ~19 hours**

---

## 📦 Expected Deliverables

After full implementation, users will be able to:

### 1. Pull Pre-Built Image

```bash
docker pull ghcr.io/thousandeyes/am-mcp-server:latest
```

### 2. Run with Single Command

```bash
docker run -p 6080:6080 \
  -e DATABASE_PASSWORD=secret \
  ghcr.io/thousandeyes/am-mcp-server:latest
```

### 3. Connect from Cursor/Claude Desktop

**Simple one-line config:**

```json
{
  "am-mcp": {
    "command": "docker",
    "args": ["run", "-i", "--rm", "am-mcp-server:latest", "--stdio"]
  }
}
```

### 4. Deploy to Production

```bash
# Option A: Docker Compose
docker-compose -f docker-compose.production.yml up -d

# Option B: Kubernetes
kubectl apply -f k8s/deployment.yml

# Option C: Direct Docker
docker run -d --restart=unless-stopped \
  -p 6080:6080 \
  --name am-mcp-server-prod \
  am-mcp-server:latest
```

### 5. Monitor Health

```bash
curl http://localhost:6080/actuator/health
curl http://localhost:6080/actuator/metrics
```

---

## 🚨 Critical Decision Points

Before implementation, the following decisions need to be made:

### 1. Registry Choice

| Option | Recommendation | Notes |
|--------|----------------|-------|
| **GitHub Container Registry** | ⭐ **Recommended** | Free, integrated with repo, good for open source |
| Docker Hub | Alternative | More public visibility, rate limits on free tier |
| Private Registry | Enterprise | Full control, requires infrastructure |

**Decision Required:** Which registry will be used?

### 2. MCP Connection Architecture

| Option | Best For | Implementation Complexity |
|--------|----------|---------------------------|
| **Option A: HTTP Mode** | Development, debugging | Low |
| **Option B: STDIO Mode** | End users, simplicity | Medium |
| **Option C: Both** | ⭐ **Recommended** | Medium-High |

**Decision Required:** Support one or both modes?

### 3. Database Strategy

| Approach | Pros | Cons |
|----------|------|------|
| **Bundle MySQL** | Easy local testing | Larger deployment |
| **External Database** | Production-ready | Requires setup |
| **Both Options** | ⭐ **Recommended** | More configs to maintain |

**Decision Required:** Bundle database or assume external?

### 4. Versioning Scheme

| Scheme | Example | Best For |
|--------|---------|----------|
| **Semantic Versioning** | 1.0.0 ⭐ **Recommended** | Clear release tracking |
| Date-based | 2025.01.15 | Time-based releases |
| Git Hash | sha-abc123 | Development builds |

**Decision Required:** How will versions be tagged?

### 5. GitHub Organization/Username

**Required for image naming:**

```bash
ghcr.io/{USERNAME_OR_ORG}/am-mcp-server:latest
#        ^^^^^^^^^^^^^^^^^
#        This needs to be decided
```

**Decision Required:** What GitHub username/org will be used?

---

## 📝 Implementation Checklist

When ready to implement, follow this checklist:

### Phase 1: Core Fixes (Day 1)

- [ ] Update `Dockerfile` port to 6080
- [ ] Update `Dockerfile.simple` to Java 17 and port 6080
- [ ] Update `docker-compose.yml` port to 6080
- [ ] Create `docker/entrypoint.sh`
- [ ] Update `Dockerfile` to include Python client
- [ ] Test locally: HTTP mode
- [ ] Test locally: STDIO mode

### Phase 2: Build Infrastructure (Day 2)

- [ ] Decide on registry (GitHub/Docker Hub/Private)
- [ ] Update `build-docker.sh` with registry config
- [ ] Add version management to build script
- [ ] Create `docker-compose.production.yml`
- [ ] Test build script end-to-end
- [ ] Push first test image to registry

### Phase 3: Documentation (Day 3)

- [ ] Update README.md with Docker section
- [ ] Create `docs/deployment-guide.md`
- [ ] Create `examples/cursor-config-docker.json`
- [ ] Update `examples/claude-desktop-config-docker.json`
- [ ] Create `CHANGELOG.md`
- [ ] Create `.env.example`
- [ ] Test all example configurations

### Phase 4: CI/CD (Day 4-5)

- [ ] Create `.github/workflows/docker-build-push.yml`
- [ ] Create `.github/workflows/test.yml`
- [ ] Set up GitHub Container Registry access
- [ ] Test automated builds on push
- [ ] Set up automated testing
- [ ] Configure security scanning

### Phase 5: Testing & Release (Day 6)

- [ ] Run full integration test suite
- [ ] Test with real Cursor installation
- [ ] Test with Claude Desktop
- [ ] Performance testing
- [ ] Security audit
- [ ] Tag version 1.0.0
- [ ] Publish to registry
- [ ] Announce release

---

## 🔗 Related Files

Files that will need updates:

- `Dockerfile` - Main production build
- `Dockerfile.simple` - Simple build
- `docker-compose.yml` - Local development
- `build-docker.sh` - Build automation
- `README.md` - User documentation
- `examples/claude-desktop-config.json` - Example configs
- All configuration files with port references

Files to create:

- `docker/entrypoint.sh` - Smart entrypoint
- `docker-compose.production.yml` - Production setup
- `Dockerfile.mcp-client` - Client-focused image
- `docs/deployment-guide.md` - Deployment docs
- `examples/cursor-config-docker.json` - Cursor example
- `.github/workflows/docker-build-push.yml` - CI/CD
- `tests/docker-integration-test.sh` - Testing
- `CHANGELOG.md` - Version history
- `.env.example` - Configuration template

---

## 📞 Questions & Next Steps

### Questions to Answer

1. **What is the target GitHub organization/username for the image?**
   - Needed for: `ghcr.io/USERNAME/am-mcp-server`

2. **Should the image be public or private?**
   - Public: Anyone can pull
   - Private: Requires authentication

3. **What's the timeline for implementation?**
   - High priority items: 2-3 days
   - Full implementation: 1-2 weeks

4. **Are there any company-specific registry requirements?**
   - Corporate registry policies
   - Security scanning requirements
   - Compliance considerations

5. **What's the expected user base?**
   - Internal developers only
   - External users/customers
   - Open source community

### Next Steps

When ready to proceed:

1. Review and approve this plan
2. Make decisions on critical points above
3. Create implementation branch
4. Start with Phase 1 (Core Fixes)
5. Iterate through phases with testing
6. Deploy and gather feedback

---

## 📚 Additional Resources

### Docker Best Practices
- [Docker Multi-stage Builds](https://docs.docker.com/build/building/multi-stage/)
- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)
- [Dockerfile Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)

### GitHub Container Registry
- [Working with Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Publishing Docker Images](https://docs.github.com/en/actions/publishing-packages/publishing-docker-images)

### MCP Protocol
- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/)
- [MCP SDKs and Tools](https://github.com/modelcontextprotocol)

---

**Document Status:** Planning Phase  
**Next Review Date:** TBD  
**Owner:** TBD  
**Last Updated:** October 17, 2025


