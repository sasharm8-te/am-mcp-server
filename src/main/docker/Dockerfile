# Account Management MCP Server
FROM artifactory.thousandeyes.com/docker/k8s-java:21-20250421

# Install curl and Python for MCP client
RUN apt-get update && apt-get install -y \
    curl \
    python3 \
    python3-pip \
    && pip3 install --no-cache-dir requests \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the pre-built jar file
COPY resources/*.jar app.jar

# Copy MCP client script  
COPY resources/cursor-mcp-client.py /app/scripts/cursor-mcp-client.py
RUN chmod +x /app/scripts/cursor-mcp-client.py

# Copy entrypoint script
COPY resources/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Change ownership to non-root user
RUN chown -R appuser:appuser /app
USER appuser

# Expose port 6080
EXPOSE 6080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:6080/actuator/health || exit 1

# Use smart entrypoint
ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["http"]
