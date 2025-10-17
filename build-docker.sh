#!/bin/bash

# CUI Integration MCP Server Docker Build Script

# Configuration
IMAGE_NAME="cui-integration-mcp-server"
VERSION="1.0.0"
REGISTRY="your-registry.com"  # Replace with your registry

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

