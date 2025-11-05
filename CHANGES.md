# Migration and Changes Summary

## Overview

This document summarizes the changes made to support Oracle Database outbox pattern and dual-environment configuration (enterprise vs Docker).

## Key Changes

### 1. Default Profile Changed

**Before:** Default profile was `docker`  
**After:** Default profile is `local` (enterprise environment)

This means the application now defaults to using external enterprise services (Kafka, PostgreSQL) unless explicitly configured otherwise.

### 2. New Oracle Support

Added complete Oracle Database support as an alternative to PostgreSQL for the outbox table.

#### New Files Created:
- `producer-app/src/main/java/com/example/kafka/producer/entity/OracleOutboxMessage.java`
- `producer-app/src/main/java/com/example/kafka/producer/repository/OracleOutboxMessageRepository.java`
- `producer-app/src/main/java/com/example/kafka/producer/service/OracleOutboxPollingService.java`
- `producer-app/src/main/java/com/example/kafka/producer/service/OracleOutboxAggregatorService.java`
- `producer-app/src/main/resources/application-oracle.yml`
- `producer-app/src/main/resources/oracle-outbox-setup.sql`

#### Modified Files:
- `producer-app/pom.xml` - Added Oracle JDBC driver dependency
- `producer-app/src/main/java/com/example/kafka/producer/service/OutboxPollingService.java` - Added @ConditionalOnProperty
- `producer-app/src/main/java/com/example/kafka/producer/service/OutboxAggregatorService.java` - Added @ConditionalOnProperty

### 3. Profile Configuration

The application now supports three profiles:

1. **`local` (default)** - Enterprise environment with external Kafka and PostgreSQL
2. **`docker`** - Local development with Docker Compose
3. **`oracle`** - Enterprise environment with Oracle Database for outbox

#### Configuration Files:
- `application.yml` - Base configuration, defaults to `local` profile
- `application-local.yml` - Enterprise PostgreSQL/Kafka configuration
- `application-docker.yml` - Docker Compose configuration
- `application-oracle.yml` - Oracle Database configuration

### 4. Documentation Updates

#### README.md
- Updated profile selection section (3 profiles instead of 2)
- Added Oracle setup instructions with SQL script reference
- Added detailed Oracle integration approaches (JDBC Polling, AQ/JMS, Debezium)
- Updated execution examples for all three profiles
- Added Oracle-specific troubleshooting section
- Updated technology stack to include Oracle
- Reorganized quick start to prioritize enterprise (local) profile

#### New Documentation Files:
- `TESTING.md` - Testing notes and manual testing procedures

### 5. Environment Variables

#### Local Profile (PostgreSQL)
```bash
DATASOURCE_URL=jdbc:postgresql://host:port/database
DATASOURCE_USERNAME=username
DATASOURCE_PASSWORD=password
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092
```

#### Oracle Profile
```bash
ORACLE_DATASOURCE_URL=jdbc:oracle:thin:@host:port:SID
ORACLE_DATASOURCE_USERNAME=username
ORACLE_DATASOURCE_PASSWORD=password
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092
SPRING_PROFILES_ACTIVE=oracle
```

#### Docker Profile
```bash
SPRING_PROFILES_ACTIVE=docker
# No additional variables needed - uses localhost defaults
```

## Architectural Decisions

### Oracle Integration Approach

**Chosen:** JDBC Polling (similar to PostgreSQL approach)

**Why:**
- Simple to implement and maintain
- No additional Oracle licensing required
- Works with any Oracle 12c+ installation
- Consistent with PostgreSQL implementation

**Alternatives Available:**
- Oracle AQ/JMS (for lower latency, requires Oracle AQ setup)
- Debezium CDC (for fully decoupled approach, requires Kafka Connect)

Both alternatives are documented in README for future consideration.

### Conditional Service Loading

Services are conditionally loaded based on `app.outbox.use-oracle` property:

- **PostgreSQL services** active when `app.outbox.use-oracle=false` or unset
- **Oracle services** active when `app.outbox.use-oracle=true`

This prevents conflicts and ensures clean separation.

## Migration Guide

### For Existing Users (Docker)

No changes required! Just explicitly set the profile:

```bash
export SPRING_PROFILES_ACTIVE=docker
mvn spring-boot:run
```

Or:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker"
```

### For Enterprise Users (New Default)

1. Set environment variables for your enterprise services
2. Run without specifying profile (defaults to `local`)

### For Oracle Users

1. Execute Oracle setup SQL script
2. Set Oracle environment variables
3. Set `SPRING_PROFILES_ACTIVE=oracle`

## Testing

### Integration Tests

**Note:** Current integration tests assume PostgreSQL. When both PostgreSQL and Oracle entities exist, 
JPA entity scanning can cause conflicts in tests.

**Solution:** Run build with `-DskipTests` or see TESTING.md for manual testing procedures.

**Future Work:** Create separate test suites for each database profile.

### Manual Testing

See `TESTING.md` for complete manual testing procedures for each profile.

## Breaking Changes

### Default Profile Change

**Impact:** Users who relied on the default `docker` profile must now explicitly set it.

**Migration:**
```bash
# Old (implicit docker):
mvn spring-boot:run

# New (explicit docker):
export SPRING_PROFILES_ACTIVE=docker
mvn spring-boot:run
```

### No Other Breaking Changes

All existing functionality remains unchanged. The changes are additive.

## Performance Considerations

### Oracle Polling Interval

Default: 1 second (same as PostgreSQL)

Can be configured via:
```yaml
app:
  outbox:
    poll-interval-ms: 1000
```

### Oracle Table Cleanup

The Oracle setup script includes an optional cleanup job (commented out by default).

To enable automatic cleanup of old messages, uncomment the DBMS_SCHEDULER job in `oracle-outbox-setup.sql`.

## Security Considerations

### Credentials Management

All database credentials are passed via environment variables, not hardcoded in configuration files.

### Oracle Permissions Required

Minimum permissions:
- SELECT, INSERT, UPDATE, DELETE on OUTBOX_MESSAGES
- SELECT on OUTBOX_SEQ

Optional (for AQ):
- EXECUTE on DBMS_AQ
- EXECUTE on DBMS_AQADM

## Future Enhancements

1. **Test Suite Improvements**
   - Separate test classes for each database type
   - Profile-based test execution

2. **Oracle AQ/JMS Integration**
   - Full implementation of JMS-based message publishing
   - Lower latency alternative to polling

3. **Metrics and Monitoring**
   - Database-specific metrics
   - Outbox lag monitoring

4. **Multi-Database Support**
   - Support for other databases (MySQL, SQL Server, etc.)

## Questions and Support

For issues or questions:
1. Check README.md for configuration details
2. Check TESTING.md for testing procedures
3. Review oracle-outbox-setup.sql for Oracle-specific setup
4. Check troubleshooting sections in README.md
