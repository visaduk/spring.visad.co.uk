#!/bin/bash

# Local restart script for VisaD Spring Boot Backend
# Usage: ./restart_local.sh

echo "================================================"
echo "Spring Boot Backend Restart (Local)"
echo "================================================"

# Stop existing Spring Boot process
echo "Stopping existing Spring Boot process..."
pkill -f "spring-boot:run"
sleep 2

# Check if process is still running
if pgrep -f "spring-boot:run" > /dev/null; then
    echo "Force killing Spring Boot process..."
    pkill -9 -f "spring-boot:run"
    sleep 1
fi

echo "✓ Spring Boot stopped"

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
        echo "  Check logs: tail -f server.log"
    fi
    
    echo ""
    echo "================================================"
    echo "Spring Boot Backend is running"
    echo "================================================"
    echo "Port: 8080"
    echo "Logs: $PWD/server.log"
    echo ""
else
    echo "✗ Failed to start Spring Boot"
    echo "Check logs: tail -50 server.log"
    exit 1
fi
