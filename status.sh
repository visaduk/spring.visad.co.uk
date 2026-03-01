#!/bin/bash

# Spring Boot Status Script
# Usage: ./status.sh

echo "================================================"
echo "Spring Boot Backend Status"
echo "================================================"

# Check if running
if pgrep -f "spring-boot:run" > /dev/null; then
    PID=$(pgrep -f "spring-boot:run")
    echo "Status: ✓ RUNNING"
    echo "PID: $PID"
    echo ""
    
    # Check application health
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        HEALTH=$(curl -s http://localhost:8080/actuator/health)
        echo "Health: ✓ HEALTHY"
        echo "$HEALTH" | jq '.' 2>/dev/null || echo "$HEALTH"
    else
        echo "Health: ⚠ NOT RESPONDING (may be starting up)"
    fi
    
    echo ""
    echo "Memory Usage:"
    ps aux | grep "spring-boot:run" | grep -v grep | awk '{print "  CPU: "$3"%  Memory: "$4"%"}'
    
    echo ""
    echo "Recent Logs (last 10 lines):"
    echo "------------------------------------------------"
    tail -10 /home/VisaD/visad.co.uk/spring.visad.co.uk/server.log
    echo "------------------------------------------------"
else
    echo "Status: ✗ NOT RUNNING"
fi

echo ""
echo "Useful commands:"
echo "  Start: ./start.sh"
echo "  Stop: ./stop.sh"
echo "  Restart: ./restart.sh"
echo "  View logs: tail -f server.log"
echo "================================================"
