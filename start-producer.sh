#!/bin/bash

# Script to start the producer application
# 
# Usage:
#   ./start-producer.sh              # Use docker profile (default, starts Docker if needed)
#   ./start-producer.sh local        # Use local profile (enterprise environment)
#
# For 'local' profile, set these environment variables:
#   export DATASOURCE_URL="jdbc:postgresql://your-postgres:5432/yourdb"
#   export DATASOURCE_USERNAME="yourusername"
#   export DATASOURCE_PASSWORD="yourpassword"
#   export KAFKA_BOOTSTRAP_SERVERS="your-kafka:9092"

PROFILE="${1:-docker}"

echo "Starting Kafka Producer with profile: $PROFILE"

# Only check/start Docker if using docker profile
if [ "$PROFILE" = "docker" ]; then
    if ! docker ps | grep -q kafka; then
        echo "Kafka is not running. Starting docker-compose..."
        docker-compose up -d
        echo "Waiting for Kafka to be ready..."
        sleep 10
    fi
else
    echo "Using enterprise environment (local profile)"
    echo "Make sure these environment variables are set:"
    echo "  - DATASOURCE_URL"
    echo "  - DATASOURCE_USERNAME"
    echo "  - DATASOURCE_PASSWORD"
    echo "  - KAFKA_BOOTSTRAP_SERVERS"
fi

# Build if needed
if [ ! -f "producer-app/target/producer-app-0.0.1-SNAPSHOT.jar" ]; then
    echo "Building producer-app..."
    mvn clean install -DskipTests
fi

echo ""
echo "Starting Producer on port 8080 with profile: $PROFILE..."
java -jar producer-app/target/producer-app-0.0.1-SNAPSHOT.jar --spring.profiles.active=$PROFILE

# Or run with Maven:
# cd producer-app && mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=$PROFILE"
