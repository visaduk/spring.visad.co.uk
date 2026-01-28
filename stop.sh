#!/bin/bash

# Spring Boot Stop Script
# Usage: ./stop.sh

echo "================================================"
echo "Stopping Spring Boot Backend"
echo "================================================"

# Stop Spring Boot process (Maven run or JAR run)
pkill -f "spring-boot:run"
pkill -f "visad-api-.*.jar"
sleep 2

# Check if process is still running
if pgrep -f "spring-boot:run" > /dev/null || pgrep -f "visad-api-.*.jar" > /dev/null; then
    echo "Force killing Spring Boot process..."
    pkill -9 -f "spring-boot:run"
    pkill -9 -f "visad-api-.*.jar"
    sleep 1
fi

# Verify stopped
if pgrep -f "spring-boot:run" > /dev/null; then
    echo "✗ Failed to stop Spring Boot"
    exit 1
else
    echo "✓ Spring Boot stopped successfully"
fi
