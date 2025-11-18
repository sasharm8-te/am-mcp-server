# 🐳 Docker Setup Guide

**Status:** Most work complete, ready for testing  
**Database:** Uses existing database (no MySQL container needed)  
**Date:** November 17, 2025

---

## ✅ What's Already Done

All Docker files have been fixed and are ready to use:

- ✅ **Dockerfile** - Updated with Java 17, port 6080, Python support, STDIO mode
- ✅ **docker-compose.yml** - Simplified (no MySQL), uses application.yml for database config
- ✅ **docker/entrypoint.sh** - Created for STDIO/HTTP mode switching
- ✅ **Dockerfile.full** - Backup of original full-build version

---

## ⏳ What You Need to Do

### Option A: Local Development/Testing (Recommended First)

Just build and run locally to test:

```bash
# 1. Build JAR
./gradlew clean bootJar

# 2. Build Docker image
docker-compose build

# 3. Run
docker-compose up -d

# 4. Test
curl http://localhost:6080/actuator/health
```

**That's it!** No registry needed for local testing.

---

### Option B: Deploy with teDocker Plugin (ThousandEyes Internal)

If deploying to ThousandEyes infrastructure:

#### Step 1: Get plugin info from your team

Ask a colleague or check another TE service (like `cui-integration-service`):

```bash
cd ../cui-integration-service
grep -A5 "teDocker" build.gradle
```

You need:
- Plugin version (e.g., `1.2.3`)
- Image name format (e.g., `account-management/am-mcp-server`)

#### Step 2: Update build.gradle

Add to your `build.gradle`:

```gradle
plugins {
    // ... existing plugins ...
    id 'com.thousandeyes.docker' version 'X.X.X'  // Replace with actual version
}

// ... rest of file ...

// Add at the end:
teDocker {
    imageName 'account-management/am-mcp-server'  // Adjust based on TE convention
}
```

#### Step 3: Build and Tag

```bash
# Build Docker image with te-docker plugin
./gradlew buildImage

# Tag the image (the plugin builds but doesn't auto-tag without registry config)
IMAGE_ID=$(docker images -q | head -1)
docker tag $IMAGE_ID accounting/am-mcp-server:latest
docker tag $IMAGE_ID accounting/am-mcp-server:1.0.0

# Verify
docker images | grep am-mcp-server
```

**Available tasks:**
- `buildImage` - Builds the Docker image
- `pushImage` - Pushes to configured registry (requires registry setup)
- `dockerBuildEnv` - Sets up Docker build environment
- `kubernetesConfig` - Generate Kubernetes config

**Note:** The plugin builds the image but doesn't auto-tag it unless a registry is configured in the `teDocker` block.

---

## 📁 Project Structure

```
am-mcp-server/
├── Dockerfile                  ✅ Ready (Java 17, port 6080, Python, STDIO)
├── Dockerfile.full             ✅ Backup of original
├── docker-compose.yml          ✅ Ready (no MySQL, uses application.yml)
├── docker/
│   ├── entrypoint.sh          ✅ Ready (STDIO support)
│   └── init-db.sql            (not used - you have existing DB)
├── build-docker.sh             (optional - for manual builds)
└── build.gradle                ⏳ Update if using teDocker
```

---

## 🧪 Testing

### Build and Run

```bash
# Build JAR
./gradlew clean bootJar

# Build Docker
docker-compose build

# Start services
docker-compose up -d

# Wait for startup
sleep 20
```

### Test Health

```bash
curl http://localhost:6080/actuator/health
```

**Expected:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### Test MCP Tools

```bash
curl -X POST http://localhost:6080/mcp/tools/list \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}'
```

Should return 25+ tools.

### Test STDIO Mode

```bash
echo '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}' | \
  docker run -i --rm am-mcp-server:latest --stdio
```

### Check Database Connection

```bash
docker-compose logs am-mcp-server | grep -i database
```

Should show successful connection to your AWS RDS database.

---

## 🎯 Using with Cursor/Claude

### Cursor (STDIO mode)

Update `~/.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "am-mcp": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "am-mcp-server:latest", "--stdio"]
    }
  }
}
```

### Claude Desktop (HTTP mode)

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

---

## 🚀 Team Deployment

### Each Team Member Runs Locally

```bash
git clone <repo>
cd am-mcp-server
docker-compose up -d
# Available at http://localhost:6080
```

### Shared Server

```bash
docker run -d \
  --name am-mcp-server \
  -p 6080:6080 \
  --restart unless-stopped \
  am-mcp-server:latest
```

### Using teDocker (ThousandEyes)

After setup in build.gradle:

```bash
# Build the image
./gradlew buildImage

# Tag it manually (until registry is configured)
IMAGE_ID=$(docker images -q | head -1)
docker tag $IMAGE_ID accounting/am-mcp-server:latest
docker tag $IMAGE_ID accounting/am-mcp-server:1.0.0
```

To push to TE registry, configure the registry in `teDocker` block and use `./gradlew pushImage`.

---

## 🔌 Using with MCP Clients (Cursor, Claude Desktop, etc.)

### Option 1: STDIO Mode (Recommended for MCP clients)

Add to your MCP client configuration file:
- **Cursor**: `~/Library/Application Support/Cursor/User/globalStorage/saoudrizwan.claude-dev/settings/cline_mcp_settings.json`
- **Claude Desktop**: `~/Library/Application Support/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "am-mcp-server": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--network=host",
        "-e", "SPRING_PROFILES_ACTIVE=local",
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

**What this does:**
- `--rm`: Removes container after it exits
- `-i`: Interactive mode (required for STDIO)
- `--network=host`: Container shares host network (can access localhost:6080)
- `-e SPRING_PROFILES_ACTIVE=local`: Sets Spring profile
- `--stdio`: Runs the MCP server in STDIO mode (using the entrypoint.sh script)

### Option 2: HTTP Mode (For direct API access)

Run the server as a persistent service:

```bash
docker run -d \
  --name am-mcp-server \
  -p 6080:6080 \
  -e SPRING_PROFILES_ACTIVE=local \
  accounting/am-mcp-server:latest
```

Then connect via HTTP at `http://localhost:6080/mcp`

### Option 3: Docker Compose (For team development)

Use the provided `docker-compose.yml`:

```bash
docker-compose up -d
```

Configure MCP client to connect to `http://localhost:6080/mcp`

---

## 🆘 Troubleshooting

### Port already in use
```bash
lsof -i :6080  # Find what's using the port
```

### Database connection failed
Check `src/main/resources/application.yml` has correct database credentials.

### Build fails
```bash
./gradlew clean
docker-compose build --no-cache
```

### View logs
```bash
docker-compose logs -f am-mcp-server
```

---

## ✅ Quick Checklist

### For Local Testing
- [ ] Build: `./gradlew clean bootJar`
- [ ] Build Docker: `docker-compose build`
- [ ] Run: `docker-compose up -d`
- [ ] Test: `curl http://localhost:6080/actuator/health`
- [ ] Test: MCP tools endpoint
- [ ] Test: STDIO mode
- [ ] Success! ✅

### For teDocker Deployment
- [ ] Get plugin version from team ✅ (Already have: 4.10.0)
- [ ] Get image naming convention ✅ (Already configured)
- [ ] Update build.gradle with plugin ✅ (Already added)
- [ ] Add teDocker config block ✅ (Already added)
- [ ] Add pluginManagement to settings.gradle ✅ (Already added)
- [ ] Test: `./gradlew buildImage`
- [ ] Verify image built successfully
- [ ] Success! ✅

---

## 📝 Key Details

### Docker Configuration
- **Port:** 6080 (not 8080)
- **Java:** 17 JRE (smaller image)
- **Database:** Connects to existing DB via application.yml
- **STDIO:** Supported via entrypoint.sh
- **Python:** Installed for MCP client bridge

### What Changed
- Removed MySQL container (you have existing database)
- Fixed port from 8080 to 6080
- Updated Java from 11 to 17
- Added Python for STDIO support
- Added smart entrypoint for HTTP/STDIO modes

### Files You Use
- **Dockerfile** - Simple, optimized build (formerly Dockerfile.simple)
- **Dockerfile.full** - Full build version (backup)
- **docker-compose.yml** - Local development
- **docker/entrypoint.sh** - Mode switching

---

## 🎉 Success!

Everything is ready. Just:

1. **Test locally:** `docker-compose up -d`
2. **Deploy with teDocker:** Add plugin to build.gradle
3. **Use with Cursor:** Configure mcp.json

**Total time:** 15-30 minutes

---

**Questions?** The code is ready to run. Just follow the testing steps above!
