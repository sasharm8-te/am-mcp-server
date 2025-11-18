#!/bin/bash

# Account Management MCP Server Docker Build Script

# Configuration
IMAGE_NAME="am-mcp-server"
VERSION="1.0.0"
# Replace with your registry:
# - GitHub: ghcr.io/YOUR_GITHUB_ORG
# - Docker Hub: docker.io/YOUR_USERNAME (or just YOUR_USERNAME)
# - Self-hosted: your-server.com:5000
REGISTRY="ghcr.io/YOUR_GITHUB_ORG"

# Build the Docker image
echo "Building Docker image..."
docker build -t ${IMAGE_NAME}:${VERSION} .
docker build -t ${IMAGE_NAME}:latest .

# Tag for registry
echo "Tagging for registry..."
docker tag ${IMAGE_NAME}:${VERSION} ${REGISTRY}/${IMAGE_NAME}:${VERSION}
docker tag ${IMAGE_NAME}:latest ${REGISTRY}/${IMAGE_NAME}:latest

# Push to registry (uncomment when ready)
# echo "Pushing to registry..."
# docker push ${REGISTRY}/${IMAGE_NAME}:${VERSION}
# docker push ${REGISTRY}/${IMAGE_NAME}:latest

echo "Build complete!"
echo "Local images:"
docker images | grep ${IMAGE_NAME}

# Test run (uncomment to test locally)
# echo "Starting test container..."
# docker run -d -p 8080:8080 --name cui-mcp-server-test ${IMAGE_NAME}:latest

