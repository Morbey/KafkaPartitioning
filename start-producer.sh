#!/bin/bash

# Script to start the producer application
# Usage: ./start-producer.sh [profile]
#   profile: docker (default) or local

PROFILE=${1:-docker}

echo "Starting Kafka Producer with profile: $PROFILE"

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
if [ ! -f "producer-app/target/producer-app-0.0.1-SNAPSHOT.jar" ]; then
    echo "Building producer-app..."
    mvn clean install -DskipTests
fi

echo ""
echo "Starting Producer on port 8080 with profile: $PROFILE..."
java -jar -Dspring.profiles.active=$PROFILE producer-app/target/producer-app-0.0.1-SNAPSHOT.jar

# Or run with Maven:
# cd producer-app && mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE
