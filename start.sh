#!/bin/bash

# Spring Boot Start Script
# Usage: ./start.sh

echo "================================================"
echo "Starting Spring Boot Backend"
echo "================================================"

# Check if already running
if pgrep -f "spring-boot:run" > /dev/null; then
    echo "⚠ Spring Boot is already running"
    echo "Use ./restart.sh to restart or ./stop.sh to stop first"
    exit 1
fi

# Navigate to project directory
cd /home/VisaD/visad.co.uk/spring.visad.co.uk

# Load env vars (encryption key, etc.)
if [ -f .env ]; then
    source .env
fi

# Start Spring Boot in background
echo "Starting Spring Boot..."
nohup mvn spring-boot:run > server.log 2>&1 &

# Wait a moment for startup
sleep 3

# Check if process started
if pgrep -f "spring-boot:run" > /dev/null; then
    echo "✓ Spring Boot started successfully"
    echo ""
    echo "Waiting for application to be ready..."
    sleep 15
    
    # Check if application is responding
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✓ Application is healthy and ready!"
    else
        echo "⚠ Application started but may still be initializing..."
        echo "  Check logs: tail -f /home/VisaD/visad.co.uk/spring.visad.co.uk/server.log"
    fi
    
    echo ""
    echo "================================================"
    echo "Spring Boot Backend is running"
    echo "================================================"
    echo "Port: 8080"
    echo "Logs: /home/VisaD/visad.co.uk/spring.visad.co.uk/server.log"
    echo "================================================"
else
    echo "✗ Failed to start Spring Boot"
    echo "Check logs: tail -50 /home/VisaD/visad.co.uk/spring.visad.co.uk/server.log"
    exit 1
fi
