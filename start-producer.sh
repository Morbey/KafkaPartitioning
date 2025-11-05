#!/bin/bash

# Script to start the producer application

echo "Starting Kafka Producer..."

# Check if docker-compose is running
if ! docker ps | grep -q kafka; then
    echo "Kafka is not running. Starting docker-compose..."
    docker-compose up -d
    echo "Waiting for Kafka to be ready..."
    sleep 10
fi

# Build if needed
if [ ! -f "producer-app/target/producer-app-0.0.1-SNAPSHOT.jar" ]; then
    echo "Building producer-app..."
    mvn clean install -DskipTests
fi

echo ""
echo "Starting Producer on port 8080..."
java -jar producer-app/target/producer-app-0.0.1-SNAPSHOT.jar

# Or run with Maven:
# cd producer-app && mvn spring-boot:run
