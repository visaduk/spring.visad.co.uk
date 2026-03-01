#!/bin/bash

# Spring Boot Restart Script
# Usage: ./restart.sh

echo "================================================"
echo "Spring Boot Backend Restart"
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
    echo ""
    echo "Useful commands:"
    echo "  View logs: tail -f /home/VisaD/visad.co.uk/spring.visad.co.uk/server.log"
    echo "  Stop: pkill -f 'spring-boot:run'"
    echo "  Check status: ps aux | grep spring-boot"
    echo "================================================"
else
    echo "✗ Failed to start Spring Boot"
    echo "Check logs: tail -50 /home/VisaD/visad.co.uk/spring.visad.co.uk/server.log"
    exit 1
fi
