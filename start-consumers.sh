#!/bin/bash

# Script to start multiple consumer instances for testing
# Usage: ./start-consumers.sh [profile]
#   profile: docker (default) or local

PROFILE=${1:-docker}

echo "Starting Kafka Consumer instances with profile: $PROFILE..."

# Only check for docker if using docker profile
if [ "$PROFILE" = "docker" ]; then
    # Check if docker-compose is running
    if ! docker ps | grep -q kafka; then
        echo "Kafka is not running. Starting docker-compose..."
        docker-compose up -d
        echo "Waiting for Kafka to be ready..."
        sleep 10
    fi
fi

# Build if needed
if [ ! -f "consumer-app/target/consumer-app-0.0.1-SNAPSHOT.jar" ]; then
    echo "Building consumer-app..."
    mvn clean install -DskipTests
fi

echo ""
echo "Starting Consumer 1 on port 8081 with profile: $PROFILE..."
java -jar -Dspring.profiles.active=$PROFILE consumer-app/target/consumer-app-0.0.1-SNAPSHOT.jar --server.port=8081 &
CONSUMER1_PID=$!

sleep 5

echo ""
echo "Starting Consumer 2 on port 8082 with profile: $PROFILE..."
java -jar -Dspring.profiles.active=$PROFILE consumer-app/target/consumer-app-0.0.1-SNAPSHOT.jar --server.port=8082 &
CONSUMER2_PID=$!

sleep 5

echo ""
echo "Starting Consumer 3 on port 8083 with profile: $PROFILE..."
java -jar -Dspring.profiles.active=$PROFILE consumer-app/target/consumer-app-0.0.1-SNAPSHOT.jar --server.port=8083 &
CONSUMER3_PID=$!

echo ""
echo "========================================="
echo "Consumers started successfully with profile: $PROFILE"
echo "Consumer 1: http://localhost:8081 (PID: $CONSUMER1_PID)"
echo "Consumer 2: http://localhost:8082 (PID: $CONSUMER2_PID)"
echo "Consumer 3: http://localhost:8083 (PID: $CONSUMER3_PID)"
echo "========================================="
echo ""
echo "To stop all consumers, run:"
echo "  kill $CONSUMER1_PID $CONSUMER2_PID $CONSUMER3_PID"
echo ""
echo "To view logs:"
echo "  tail -f consumer-app/logs/*.log"
echo ""
echo "Press Ctrl+C to stop this script (consumers will keep running)"
echo ""

# Save PIDs to file for easy cleanup
echo "$CONSUMER1_PID" > /tmp/kafka-consumer-pids.txt
echo "$CONSUMER2_PID" >> /tmp/kafka-consumer-pids.txt
echo "$CONSUMER3_PID" >> /tmp/kafka-consumer-pids.txt

# Wait for user interrupt
wait
