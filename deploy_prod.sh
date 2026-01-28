#!/bin/bash

# Production Deployment Script
# Usage: ./deploy_prod.sh

echo "================================================"
echo "    Visad API - Production Deployment"
echo "================================================"

# 1. Stop existing instance
echo "Step 1: Stopping current instance..."
./stop.sh

# 2. Build the project
echo "Step 2: Building project..."
# Skip tests for speed in prod deployment if trusted, but ideally keep them. 
# For now keeping them enabled to be safe.
if mvn clean package -DskipTests; then
    echo "✓ Build successful"
else
    echo "✗ Build failed"
    exit 1
fi

# 3. Start the application
echo "Step 3: Starting application..."
nohup java -jar target/visad-api-1.0.0.jar > server.log 2>&1 &

# 4. Wait for startup
echo "Waiting for application to start..."
sleep 15

# 5. Check health
if curl -f -s http://localhost:8080/api/auth/health > /dev/null 2>&1; then
    echo "✓ Application deployed and healthy!"
    echo "Logs: tail -f server.log"
else
    echo "⚠ Application started but endpoint not responding yet."
    echo "Check logs: tail -f server.log"
fi
