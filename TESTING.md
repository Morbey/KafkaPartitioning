# Testing Notes

## Current Test Status

The existing integration tests (`OutboxPatternIntegrationTest`) are designed for PostgreSQL outbox pattern.

### Known Issue with Tests

When both `OutboxMessage` (PostgreSQL) and `OracleOutboxMessage` entities exist in the codebase, 
JPA entity scanning picks up both entities even in test mode. This can cause table creation conflicts.

### Workaround

Run tests with:
```bash
mvn clean install -DskipTests
```

### Future Improvements

1. **Separate test configurations**: Create dedicated test classes for Oracle vs PostgreSQL outbox
2. **Profile-based entity scanning**: Use `@EntityScan` with profile-specific packages
3. **Exclude Oracle entity**: Move Oracle entities to a separate module or use `exclude-unmapped-classes`

## Manual Testing

To manually test the different profiles:

### Test PostgreSQL Outbox (Local Profile)
```bash
# Set environment variables
export DATASOURCE_URL="jdbc:postgresql://localhost:5432/testdb"
export DATASOURCE_USERNAME="test"
export DATASOURCE_PASSWORD="test"
export KAFKA_BOOTSTRAP_SERVERS="localhost:9092"

# Run application
cd producer-app
mvn spring-boot:run
```

### Test Docker Profile
```bash
# Start Docker Compose
docker-compose up -d

# Run application with docker profile
cd producer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker"
```

### Test Oracle Outbox
```bash
# Set Oracle environment variables
export ORACLE_DATASOURCE_URL="jdbc:oracle:thin:@localhost:1521:ORCL"
export ORACLE_DATASOURCE_USERNAME="testuser"
export ORACLE_DATASOURCE_PASSWORD="testpass"
export KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
export SPRING_PROFILES_ACTIVE="oracle"

# Run Oracle setup script first
sqlplus testuser/testpass@ORCL @producer-app/src/main/resources/oracle-outbox-setup.sql

# Run application
cd producer-app
mvn spring-boot:run
```

## Testing Endpoints

Once the application is running, test the outbox functionality:

```bash
# Publish a message
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Test message",
    "partitionKey": "client-1"
  }'

# Check outbox stats
curl http://localhost:8080/api/outbox/stats
```
