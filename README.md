# Kafka Partitions PoC - Modern Setup without Zookeeper

This project demonstrates a complete Kafka application using Spring Kafka (without Spring Cloud Stream), with persistence in PostgreSQL/Oracle using Hibernate 6, Transactional Outbox pattern, **snapshot aggregation per task**, and monitoring with Prometheus and Grafana.

## 🎯 Key Features

- ✅ **Kafka in KRaft mode** - No Zookeeper dependency
- ✅ **Complete persistence** - PostgreSQL with Hibernate 6
- ✅ **Oracle support** - Outbox in Oracle Database with JDBC polling or Oracle AQ/JMS
- ✅ **Outbox pattern** - Transactional message production
- ✅ **Task aggregation** - Complete snapshots instead of per-attribute messages
- ✅ **Materialized read-model** - `task_snapshots` table for efficient queries
- ✅ **Data hierarchy** - Task → TaskAttribute → TaskAttributeValue
- ✅ **Simulated processing** - Configurable delay (2-20 seconds)
- ✅ **Rebalance prevention** - Optimized configurations for long processing
- ✅ **Graceful shutdown** - Endpoint to stop consumption before terminating pod
- ✅ **Monitoring** - Prometheus + Grafana with custom metrics
- ✅ **Integration tests** - Testcontainers with Kafka and PostgreSQL
- ✅ **Partition distribution** - Messages distributed by key (client)
- ✅ **Multi-environment** - Support for local Docker, enterprise PostgreSQL and Oracle Database
- ✅ **Enterprise default profile** - Configured to use external services without Docker

## 📋 Project Structure

```
kafkaPartitionsPoc/
├── consumer-app/          # Consumer application with persistence
│   ├── entity/           # Task, TaskAttribute, TaskAttributeValue, MessageRecord
│   ├── repository/       # Spring Data JPA repositories
│   ├── service/          # TaskConsumerService with 2-20s processing
│   ├── config/           # Kafka consumer config with rebalance prevention
│   └── controller/       # Endpoint /internal/stop-consuming
├── producer-app/          # Producer application with Outbox pattern
│   ├── entity/           # OutboxMessage
│   ├── repository/       # OutboxMessageRepository
│   ├── service/          # OutboxPollingService (scheduler)
│   ├── controller/       # REST API to add messages to outbox
│   └── config/           # Kafka producer config
├── monitoring/            # Prometheus + Grafana configurations
└── docker-compose.yml     # Kafka (KRaft), PostgreSQL, Prometheus, Grafana
```

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker and Docker Compose (for local environment)
- **OR** access to external Kafka and PostgreSQL (enterprise environment)

### Choose Execution Profile

This project supports three execution profiles:

#### 1. **`local` Profile** (default) - Enterprise Environment (without Docker)
Uses external Kafka and PostgreSQL configured via environment variables.
**This is the default profile** - ideal for professional enterprise environments.

#### 2. **`docker` Profile** - Local Environment with Docker
Uses Kafka and PostgreSQL launched locally via `docker-compose`.
Use this profile only when explicitly requested for local development.

#### 3. **`oracle` Profile** - Environment with Oracle Database
Uses Oracle Database for the outbox table, with external Kafka.
Ideal for environments where Oracle AQ/JMS is already in use.

### 1. Start Infrastructure

#### Option A: Enterprise Environment (`local` profile) - DEFAULT

Configure the following environment variables pointing to your servers:

```bash
# PostgreSQL configuration
export DATASOURCE_URL="jdbc:postgresql://your-enterprise-postgres:5432/yourdb"
export DATASOURCE_USERNAME="yourusername"
export DATASOURCE_PASSWORD="yourpassword"

# Kafka configuration
export KAFKA_BOOTSTRAP_SERVERS="your-enterprise-kafka:9092"

# The 'local' profile is activated automatically (default)
# To explicitly define: export SPRING_PROFILES_ACTIVE="local"
```

#### Option B: Local Environment with Docker (`docker` profile)

```bash
# First, start Docker Compose
docker-compose up -d
```

This starts:
- **Kafka** (port 9092) - KRaft mode, without Zookeeper
- **PostgreSQL** (port 5432) - database for both applications
- **Prometheus** (port 9090) - metrics collection
- **Grafana** (port 3000) - metrics visualization (admin/admin)

**Create snapshot topic (optional, will be created automatically):**
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic task-snapshots --partitions 3 --replication-factor 1 \
  --config cleanup.policy=compact
```

**To use this profile, define:**
```bash
export SPRING_PROFILES_ACTIVE="docker"
```

#### Option C: Environment with Oracle Database (`oracle` profile)

**1. Execute the Oracle setup SQL script:**
```sql
-- Execute the script in: producer-app/src/main/resources/oracle-outbox-setup.sql
-- This script creates:
-- - OUTBOX_MESSAGES table
-- - OUTBOX_SEQ sequence
-- - Performance indexes
-- - (Optional) Oracle AQ queue for JMS integration
```

**2. Configure environment variables:**
```bash
# Oracle Database configuration
export ORACLE_DATASOURCE_URL="jdbc:oracle:thin:@your-oracle:1521:ORCL"
export ORACLE_DATASOURCE_USERNAME="yourusername"
export ORACLE_DATASOURCE_PASSWORD="yourpassword"

# Kafka configuration
export KAFKA_BOOTSTRAP_SERVERS="your-enterprise-kafka:9092"

# (Optional) Oracle AQ configuration
export ORACLE_AQ_QUEUE_NAME="OUTBOX_QUEUE"
export ORACLE_AQ_QUEUE_TABLE="OUTBOX_QUEUE_TABLE"
export ORACLE_AQ_POLL_INTERVAL_MS="1000"

# Activate the 'oracle' profile
export SPRING_PROFILES_ACTIVE="oracle"
```

**Notes about Oracle:**
- Oracle outbox uses JDBC polling by default (similar to PostgreSQL)
- Oracle AQ (Advanced Queuing) is optional and can be configured for JMS integration
- The outbox table uses CLOB for large payloads
- Automatic cleanup of old messages can be configured (see SQL script)


### 2. Build the Project

```bash
mvn clean install
```

### 3. Run Producer

#### With Enterprise `local` profile (default):
```bash
cd producer-app
# Assuming environment variables are already configured (see section 1)
mvn spring-boot:run
```

**Or** with inline environment variables:
```bash
cd producer-app
DATASOURCE_URL="jdbc:postgresql://your-postgres:5432/yourdb" \
DATASOURCE_USERNAME="yourusername" \
DATASOURCE_PASSWORD="yourpassword" \
KAFKA_BOOTSTRAP_SERVERS="your-kafka:9092" \
mvn spring-boot:run
```

#### With Docker profile:
```bash
cd producer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker"
```

#### With Oracle profile:
```bash
cd producer-app
SPRING_PROFILES_ACTIVE=oracle \
ORACLE_DATASOURCE_URL="jdbc:oracle:thin:@your-oracle:1521:ORCL" \
ORACLE_DATASOURCE_USERNAME="yourusername" \
ORACLE_DATASOURCE_PASSWORD="yourpassword" \
KAFKA_BOOTSTRAP_SERVERS="your-kafka:9092" \
mvn spring-boot:run
```

The producer will be available at http://localhost:8080

### 4. Run Consumer(s)

#### With Enterprise `local` profile (default):

**Terminal 1 (Consumer 1):**
```bash
cd consumer-app
# Assuming environment variables are already configured
mvn spring-boot:run
```

**Terminal 2 (Consumer 2 - optional):**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

**Terminal 3 (Consumer 3 - optional):**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```

#### With Docker profile:

**Terminal 1 (Consumer 1):**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker"
```

**Terminal 2 (Consumer 2 - optional):**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker --server.port=8082"
```

#### With Enterprise profile (inline variables):

```bash
cd consumer-app
DATASOURCE_URL="jdbc:postgresql://your-postgres:5432/yourdb" \
DATASOURCE_USERNAME="yourusername" \
DATASOURCE_PASSWORD="yourpassword" \
KAFKA_BOOTSTRAP_SERVERS="your-kafka:9092" \
mvn spring-boot:run
```

## 🏗️ Architecture for High Volume

### Scenario: Thousands of Changes per Task

When a task undergoes many changes (e.g., massive attribute updates), without aggregation each change would generate a message in Kafka, overloading the system and the frontend.

### Implemented Solution: Snapshot Aggregator

**Flow:**

```
[Producer] 
  ↓ inserts outbox (attribute A changed)
  ↓ inserts outbox (attribute B changed)
  ↓ inserts outbox (attribute C changed)
  ↓
[OutboxAggregatorService] (scheduled 500ms)
  ↓ groups by task_id
  ↓ applies debounce (200ms)
  ↓ merge: latest version of each attribute
  ↓ publishes 1 complete snapshot → topic 'task-snapshots'
  ↓ marks original messages as published
  ↓
[TaskSnapshotConsumer]
  ↓ consumes snapshot
  ↓ updates task_snapshots (read-model)
  ↓ frontend reads complete version
  ↓ (optional) notifies frontend via WebSocket
```

### Configuration for High Throughput

**Producer:**
- `aggregator-interval-ms: 500` - Aggregation frequency
- `debounce-ms: 200` - Wait window before aggregating
- Adjust according to volume (higher debounce = more aggregation, lower latency)

**Snapshot Consumer:**
- Use `task-snapshots` topic partitioned by `taskId`
- Ensure ordering by task (partition key)
- Dedicated consumer group (`task-snapshot-consumer-group`)
- Scale consumers according to partitions

**Kafka:**
- Create `task-snapshots` topic with adequate number of partitions
- Configure `cleanup.policy=compact` to retain only the latest snapshot per key
- Monitor consumer lag

### Considered Alternatives

1. **Per-attribute messages + final marker**: 
   - ❌ Complex to implement (changeSetId, seqNo, isLast)
   - ❌ Frontend needs to reconstruct state
   
2. **Kafka Streams for aggregation**:
   - ✅ Scalable and robust
   - ❌ More complex to configure and maintain
   
3. **Snapshot in Producer** (chosen):
   - ✅ Simple and effective
   - ✅ Fewer messages in Kafka
   - ✅ Frontend consumes complete state
   - ⚠️ Debounce can add latency (200ms)

## 🔌 Oracle Database Integration

The project supports Oracle Database as an alternative to PostgreSQL for the outbox table, ideal for enterprise environments that already use Oracle and/or Oracle AQ (Advanced Queuing).

### Integration Approaches

#### 1. **JDBC Polling** (Current Implementation - Recommended)

The simplest approach compatible with all Oracle environments:

```
[Application]
  ↓ transactionally inserts into OUTBOX_MESSAGES (Oracle)
  ↓
[OracleOutboxPollingService] (scheduled 1s)
  ↓ queries: SELECT * FROM OUTBOX_MESSAGES WHERE PUBLISHED = 0
  ↓ publishes messages to Kafka
  ↓ updates: UPDATE OUTBOX_MESSAGES SET PUBLISHED = 1
```

**Advantages:**
- ✅ Simple to implement and maintain
- ✅ Requires no additional Oracle configuration
- ✅ Works with any Oracle version (12c+)
- ✅ Transactional and reliable

**Disadvantages:**
- ⚠️ Polling latency (configurable, default 1s)
- ⚠️ Additional database load (periodic queries)

#### 2. **Oracle AQ/JMS** (Available - Optional)

Approach based on Oracle's native messaging, using Oracle Advanced Queuing:

```
[Application]
  ↓ transactionally inserts into OUTBOX_MESSAGES (Oracle)
  ↓ (optional trigger) enqueues message to AQ
  ↓
[Oracle AQ Queue: OUTBOX_QUEUE]
  ↓
[JMS Consumer] (in application)
  ↓ receives instant notification from AQ
  ↓ publishes to Kafka
  ↓ marks message as published
```

**Advantages:**
- ✅ Minimal latency (push notification)
- ✅ Reduces polling load on database
- ✅ Native Oracle integration

**Disadvantages:**
- ❌ Requires Oracle AQ configured and licensed
- ❌ Higher setup complexity
- ❌ Additional dependencies (Oracle AQ libraries)

**Oracle AQ Setup:**
```sql
-- See complete script in: producer-app/src/main/resources/oracle-outbox-setup.sql
BEGIN
    DBMS_AQADM.CREATE_QUEUE_TABLE(...);
    DBMS_AQADM.CREATE_QUEUE(...);
    DBMS_AQADM.START_QUEUE(...);
END;
```

#### 3. **Debezium with Oracle Connector** (External Alternative)

Use Debezium to capture changes (CDC) in the Oracle outbox table:

```
[Oracle OUTBOX_MESSAGES]
  ↓
[Debezium Oracle Connector] (via LogMiner or XStream)
  ↓ captures INSERTs via CDC
  ↓ publishes directly to Kafka
  ↓
[Kafka Topic]
```

**Advantages:**
- ✅ Decoupled from application
- ✅ Low latency
- ✅ Scalable

**Disadvantages:**
- ❌ Additional infrastructure (Kafka Connect)
- ❌ Requires special Oracle permissions (LogMiner/XStream)
- ❌ More complex to configure

### Approach Selection

**Recommendation:** Use **JDBC Polling** (current implementation) by default.

- If latency < 1s is critical: consider **Oracle AQ/JMS**
- If you prefer to decouple from code: consider **Debezium**

The project already implements JDBC Polling and has basic support for Oracle AQ (structures created in the SQL script).

## 📊 How It Works

### Outbox Pattern (Producer)

1. Client makes POST to `/api/publish` or `/api/publish-batch`
2. Message is **inserted into `outbox_messages` table** (transactional)
3. `OutboxPollingService` (scheduled every 1s) reads unpublished messages
4. Publishes to Kafka and marks as `published = true`
5. Uses `messageKey` to distribute across partitions

### Outbox Aggregation by Task (Snapshot Pattern)

To handle high volume of messages per task (e.g., multiple attribute changes), 
the system implements an aggregation pattern:

1. **OutboxAggregatorService** (scheduled every 500ms) groups outbox messages by `task_id`
2. Applies a **debounce** window (200ms by default) to wait for related messages
3. **Merge** of attributes: latest change of each attribute prevails
4. Publishes a **complete snapshot** of the task to the `task-snapshots` topic
5. Marks original messages as published

**Benefits:**
- Drastically reduces the number of messages sent to Kafka
- Frontend consumes only complete snapshots (simplifies logic)
- Maintains ordering by task (via partition key = taskId)
- Ensures atomicity (transactional)

### Consumer with Persistence

1. Receives message from Kafka (`@KafkaListener`)
2. Creates `MessageRecord` with `receivedAt` timestamp
3. **Simulates processing** (random 2-20 second delay)
4. Tries to parse as `Task` structure and persists hierarchy
5. Updates `MessageRecord` with `processedAt` and `processingDurationMs`
6. **Manual commit** of offset only after successful persistence

### Snapshot Consumer (Read-Model)

1. **TaskSnapshotConsumer** consumes from `task-snapshots` topic
2. Updates `task_snapshots` table (materialized read-model)
3. Each task has a single record with the most recent version
4. Frontend queries `task_snapshots` to get complete state
5. Notification can be sent via WebSocket after update (future work)

### Avoiding Rebalances

Configuration in `consumer-app/application.yml`:

```yaml
max.poll.interval.ms: 300000      # 5 minutes - maximum time between polls
session.timeout.ms: 60000          # 1 minute - session time
heartbeat.interval.ms: 20000       # 20 seconds - heartbeat interval
max.poll.records: 1                # 1 message per poll (fine control)
```

## 🧪 Integration Tests

Run the tests:

```bash
mvn test
```

The tests use:
- **Testcontainers** for PostgreSQL and Kafka
- **@EmbeddedKafka** for Kafka tests
- **Awaitility** for asynchronous assertions

### Consumer Tests

- Single message consumption
- Multiple messages with different keys
- Task hierarchical structure parsing
- Timestamp and duration verification

### Producer Tests

- Publishing via outbox pattern
- Distribution across partitions
- Multiple messages with different clients

## 📡 API Endpoints

### Producer App (port 8080)

#### Publish single message
```bash
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello Kafka!",
    "partitionKey": "client-1"
  }'
```

#### Publish batch of messages
```bash
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{
    "count": 30,
    "prefix": "TestMessage"
  }'
```

#### Outbox statistics
```bash
curl http://localhost:8080/api/outbox/stats
```

#### Health check
```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

### Consumer App (port 8081+)

#### Stop consumption (graceful shutdown)
```bash
curl -X POST http://localhost:8081/internal/stop-consuming
```

#### Prometheus metrics
```bash
curl http://localhost:8081/actuator/prometheus
```

## 📈 Monitoring

### Prometheus

Access at http://localhost:9090

Useful queries:
```promql
# Rate of messages processed per second
rate(kafka_consumer_fetch_manager_records_consumed_total[1m])

# Average processing duration
avg(kafka_consumer_processing_duration_ms)

# Unpublished messages in outbox
outbox_messages_unpublished_total
```

### Grafana

1. Access at http://localhost:3000 (admin/admin)
2. Prometheus datasource is already configured
3. Create custom dashboards or import templates

Exposed metrics:
- `outbox.messages.published` - Total published messages
- `outbox.messages.failed` - Total publication failures
- Standard Kafka metrics (consumer lag, throughput, etc.)
- Application metrics (JVM, CPU, memory)

## 🗄️ Database Structure

### Table: `tasks`
```sql
- id (bigserial)
- task_id (varchar, unique)
- raw_payload (text)
- created_at (timestamptz)
```

### Table: `task_attributes`
```sql
- id (bigserial)
- task_id (bigint FK)
- attribute_name (varchar)
- attribute_type (varchar) -- STRING, NUMERIC, DATE, BOOLEAN, ENTITY, TEXT
```

### Table: `task_attribute_values`
```sql
- id (bigserial)
- attribute_id (bigint FK)
- string_value (varchar)
- numeric_value (numeric)
- date_value (timestamptz)
- boolean_value (boolean)
- entity_ref (varchar)
- text_value (text)
```

### Table: `message_records`
```sql
- id (bigserial)
- raw_message (text)
- received_at (timestamptz)
- processed_at (timestamptz)
- kafka_topic (varchar)
- partition (integer)
- offset_value (bigint)
- message_key (varchar)
- processing_duration_ms (bigint)
```

### Table: `outbox_messages` (PostgreSQL)
```sql
- id (bigserial)
- payload (text)
- message_key (varchar)
- topic (varchar)
- published (boolean)
- created_at (timestamptz)
- published_at (timestamptz)
- client_id (varchar)
- task_id (varchar)          -- NEW: used for task aggregation
```

### Table: `OUTBOX_MESSAGES` (Oracle)
```sql
- ID (NUMBER(19))            -- Primary key with OUTBOX_SEQ
- PAYLOAD (CLOB)             -- JSON payload
- MESSAGE_KEY (VARCHAR2(500))
- TOPIC (VARCHAR2(255))
- PUBLISHED (NUMBER(1))      -- 0=false, 1=true
- CREATED_AT (TIMESTAMP WITH TIME ZONE)
- PUBLISHED_AT (TIMESTAMP WITH TIME ZONE)
- CLIENT_ID (VARCHAR2(255))
- TASK_ID (VARCHAR2(255))    -- Used for task aggregation
```

**Note:** For complete Oracle setup, execute the script:
`producer-app/src/main/resources/oracle-outbox-setup.sql`

### Table: `task_snapshots`
```sql
- id (bigserial)
- task_id (varchar, unique)  -- Unique task identifier
- snapshot_data (text)       -- Complete JSON snapshot
- version (bigint)           -- Snapshot version (increments on each update)
- created_at (timestamptz)   -- When it was created
- updated_at (timestamptz)   -- Last update
- kafka_offset (bigint)      -- Source Kafka offset
- kafka_partition (integer)  -- Kafka partition
```

## 🎭 Test Scenarios

### Test 1: Basic Distribution
1. Start 1 consumer
2. Publish 30 messages: `POST /api/publish-batch` with `count: 30`
3. Observe that the consumer processes from all 3 partitions
4. Check logs to see processing duration (2-20s per message)

### Test 2: Rebalancing
1. Start 1 consumer (port 8081)
2. Publish messages
3. Start 2nd consumer (port 8082) → observe rebalance in logs
4. Publish more messages → distributed between consumers
5. Stop 2nd consumer → observe rebalance again

### Test 3: Real-time Outbox Pattern
1. Insert messages directly into the outbox table:
```sql
INSERT INTO outbox_messages (payload, message_key, topic, client_id, published, created_at)
VALUES ('Manual message', 'client-1', 'task-topic', 'client-1', false, NOW());
```
2. Observe message being published automatically (in 1s)
3. Verify consumer processes the message

### Test 4: Processing with Task Structure
```bash
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{
    "message": "{\"taskId\":\"TASK-001\",\"attributes\":[{\"name\":\"priority\",\"type\":\"STRING\",\"values\":[\"HIGH\"]},{\"name\":\"amount\",\"type\":\"NUMERIC\",\"values\":[\"1500.50\"]}]}",
    "partitionKey": "client-1"
  }'
```

Verify in the database that the structure was parsed and persisted:
```sql
SELECT t.task_id, ta.attribute_name, ta.attribute_type, 
       tav.string_value, tav.numeric_value
FROM tasks t
JOIN task_attributes ta ON ta.task_id = t.id
JOIN task_attribute_values tav ON tav.attribute_id = ta.id
WHERE t.task_id = 'TASK-001';
```

## 🔧 Important Configurations

### Execution Profiles

The system supports three profiles via the `SPRING_PROFILES_ACTIVE` variable:

- **`local`** (default): Uses external Kafka and PostgreSQL via environment variables (enterprise environment without Docker)
- **`docker`**: Uses local Kafka and PostgreSQL (localhost) via docker-compose
- **`oracle`**: Uses Oracle Database for outbox with external Kafka via environment variables

### Environment Variables

#### `local` Profile (Enterprise PostgreSQL)

```bash
# PostgreSQL
DATASOURCE_URL=jdbc:postgresql://host:port/database
DATASOURCE_USERNAME=username
DATASOURCE_PASSWORD=password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092

# Active profile (optional, already the default)
SPRING_PROFILES_ACTIVE=local
```

#### `docker` Profile (Local Docker)

```bash
# Requires no environment variables - uses hardcoded values in application-docker.yml
# To activate:
SPRING_PROFILES_ACTIVE=docker
```

#### `oracle` Profile (Oracle Database)

```bash
# Oracle Database
ORACLE_DATASOURCE_URL=jdbc:oracle:thin:@host:port:SID
ORACLE_DATASOURCE_USERNAME=username
ORACLE_DATASOURCE_PASSWORD=password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092

# Oracle AQ (optional)
ORACLE_AQ_QUEUE_NAME=OUTBOX_QUEUE
ORACLE_AQ_QUEUE_TABLE=OUTBOX_QUEUE_TABLE
ORACLE_AQ_POLL_INTERVAL_MS=1000

# Active profile
SPRING_PROFILES_ACTIVE=oracle
```

### How to Switch Between Profiles

**Option 1: Environment variable**
```bash
export SPRING_PROFILES_ACTIVE=docker  # or local, or oracle
mvn spring-boot:run
```

**Option 2: Command line argument**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker"
```

**Option 3: System property**
```bash
mvn spring-boot:run -Dspring.profiles.active=docker
```

### Consumer Configurations (application.yml)

```yaml
spring.kafka.consumer:
  max-poll-records: 1                    # Process 1 msg at a time
  properties:
    max.poll.interval.ms: 300000         # 5 min - adjust as needed
    session.timeout.ms: 60000
    heartbeat.interval.ms: 20000

app.processing:
  min-delay-seconds: 2                   # Minimum delay (adjustable)
  max-delay-seconds: 20                  # Maximum delay (adjustable)
```

### Producer Configurations (application.yml)

```yaml
app.outbox:
  poll-interval-ms: 1000                 # Poll every 1 second
  batch-size: 100                        # Process up to 100 msgs at a time
  aggregator-interval-ms: 500            # Aggregator interval
  debounce-ms: 200                       # Debounce window for aggregation
  
app.kafka:
  topic: task-topic                      # Main topic
  snapshot-topic: task-snapshots         # Aggregated snapshots topic
```

## 🐳 Kubernetes Deployment

Example Deployment with graceful shutdown:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-consumer
spec:
  replicas: 3
  template:
    spec:
      terminationGracePeriodSeconds: 180
      containers:
      - name: consumer
        image: consumer-app:latest
        lifecycle:
          preStop:
            exec:
              command: 
              - /bin/sh
              - -c
              - "curl -X POST http://localhost:8081/internal/stop-consuming || true; sleep 10"
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
```

## 📚 Technologies Used

- **Java 17**
- **Spring Boot 3.1.5**
- **Spring Kafka** (not Spring Cloud Stream)
- **Hibernate 6.2.13** (Jakarta Persistence API)
- **PostgreSQL 15** (for local/docker profile)
- **Oracle Database 12c+** (for oracle profile - optional)
- **Kafka 7.5.0** (KRaft mode, without Zookeeper)
- **Prometheus + Grafana**
- **Testcontainers 1.19.1**
- **Maven**

## 🤔 Troubleshooting

### Kafka won't start in Docker
```bash
docker-compose logs kafka
# Check if port 9092 is free
# Recreate volume if necessary: docker-compose down -v
```

### Profile is not being applied correctly
```bash
# Check which profile is active in startup logs:
# Look for: "The following profiles are active: local"

# Force specific profile:
export SPRING_PROFILES_ACTIVE=docker  # or local, or oracle
mvn spring-boot:run

# Check loaded configuration:
curl http://localhost:8080/actuator/env | jq '.propertySources'
```

### Oracle: Connection error
```bash
# Check JDBC URL:
# Thin format: jdbc:oracle:thin:@hostname:port:SID
# Service format: jdbc:oracle:thin:@hostname:port/service_name
# TNS: jdbc:oracle:thin:@(DESCRIPTION=(...))

# Test connectivity:
telnet your-oracle-host 1521

# Verify user has permissions:
# - SELECT, INSERT, UPDATE, DELETE on OUTBOX_MESSAGES
# - SELECT on OUTBOX_SEQ
# - (Optional) EXECUTE on DBMS_AQ, DBMS_AQADM for Oracle AQ
```

### Oracle: OUTBOX_MESSAGES table not found
```bash
# Execute the setup script:
sqlplus username/password@SID @producer-app/src/main/resources/oracle-outbox-setup.sql

# Verify table was created:
sqlplus username/password@SID
SQL> SELECT table_name FROM user_tables WHERE table_name = 'OUTBOX_MESSAGES';
SQL> SELECT sequence_name FROM user_sequences WHERE sequence_name = 'OUTBOX_SEQ';
```

### Oracle: Messages are not being published
```bash
# Check pending messages in outbox:
sqlplus username/password@SID
SQL> SELECT COUNT(*) FROM OUTBOX_MESSAGES WHERE PUBLISHED = 0;

# Check producer logs:
# Look for: "Processing N unpublished messages from Oracle outbox"

# Verify Oracle service is active:
# Look for: "OracleOutboxPollingService" in logs
# If it doesn't appear, check if app.outbox.use-oracle=true in profile
```

### Frequent rebalances
- Increase `max.poll.interval.ms` if messages take too long
- Reduce `max-poll-records` to process fewer messages at a time
- Check if consumers are committing regularly

### Messages are not consumed
```bash
# Check consumer group offset
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group task-consumer-group

# Check topic
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic task-topic
```

### Outbox messages are not published
```sql
-- Check pending messages
SELECT * FROM outbox_messages WHERE published = false;

-- Check producer logs
# Logs should show "Publishing message X to topic Y"
```

## 📄 License

MIT License

## 👥 Contributors

Developed as PoC to demonstrate:
- Modern Kafka without Zookeeper
- Transactional Outbox pattern
- Rebalance prevention in long processing
- Complete monitoring with Prometheus/Grafana
