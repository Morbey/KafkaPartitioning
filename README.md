# Kafka Partitions PoC - Modern Setup without Zookeeper

Este projeto demonstra uma aplicação completa de Kafka usando Spring Kafka (sem Spring Cloud Stream), com persistência em PostgreSQL/Oracle usando Hibernate 6, padrão Transactional Outbox, **agregação de snapshots por task**, e monitorização com Prometheus e Grafana.

## 🎯 Características Principais

- ✅ **Kafka em modo KRaft** - Sem dependência de Zookeeper
- ✅ **Persistência completa** - PostgreSQL com Hibernate 6
- ✅ **Suporte Oracle** - Outbox em Oracle Database com polling JDBC ou Oracle AQ/JMS
- ✅ **Padrão Outbox** - Produção transacional de mensagens
- ✅ **Agregação por Task** - Snapshots completos em vez de mensagens por atributo
- ✅ **Read-Model materializado** - Tabela `task_snapshots` para consulta eficiente
- ✅ **Hierarquia de dados** - Task → TaskAttribute → TaskAttributeValue
- ✅ **Processamento simulado** - Delay configurável (2-20 segundos)
- ✅ **Prevenção de rebalances** - Configurações otimizadas para processamento longo
- ✅ **Graceful shutdown** - Endpoint para parar consumo antes de terminar o pod
- ✅ **Monitorização** - Prometheus + Grafana com métricas personalizadas
- ✅ **Testes de integração** - Testcontainers com Kafka e PostgreSQL
- ✅ **Distribuição por partições** - Mensagens distribuídas por key (cliente)
- ✅ **Multi-ambiente** - Suporte para Docker local, PostgreSQL empresarial e Oracle Database
- ✅ **Perfil padrão empresarial** - Configurado para usar serviços externos sem Docker

## 📋 Estrutura do Projeto

```
kafkaPartitionsPoc/
├── consumer-app/          # Aplicação consumidora com persistência
│   ├── entity/           # Task, TaskAttribute, TaskAttributeValue, MessageRecord
│   ├── repository/       # Spring Data JPA repositories
│   ├── service/          # TaskConsumerService com processamento 2-20s
│   ├── config/           # Kafka consumer config com rebalance prevention
│   └── controller/       # Endpoint /internal/stop-consuming
├── producer-app/          # Aplicação produtora com Outbox pattern
│   ├── entity/           # OutboxMessage
│   ├── repository/       # OutboxMessageRepository
│   ├── service/          # OutboxPollingService (scheduler)
│   ├── controller/       # REST API para adicionar mensagens ao outbox
│   └── config/           # Kafka producer config
├── monitoring/            # Configurações Prometheus + Grafana
└── docker-compose.yml     # Kafka (KRaft), PostgreSQL, Prometheus, Grafana
```

## 🚀 Quick Start

### Pré-requisitos

- Java 17+
- Maven 3.6+
- Docker e Docker Compose (para ambiente local)
- **OU** acesso a Kafka e PostgreSQL externos (ambiente empresarial)

### Escolher o Perfil de Execução

Este projeto suporta três perfis de execução:

#### 1. **Perfil `local`** (padrão) - Ambiente Empresarial (sem Docker)
Usa Kafka e PostgreSQL externos configurados via variáveis de ambiente.
**Este é o perfil padrão** - ideal para ambientes empresariais profissionais.

#### 2. **Perfil `docker`** - Ambiente Local com Docker
Usa Kafka e PostgreSQL levantados localmente via `docker-compose`.
Use este perfil apenas quando explicitamente solicitado para desenvolvimento local.

#### 3. **Perfil `oracle`** - Ambiente com Oracle Database
Usa Oracle Database para a tabela de outbox, com Kafka externo.
Ideal para ambientes onde Oracle AQ/JMS já está em uso.

### 1. Iniciar Infraestrutura

#### Opção A: Ambiente Empresarial (perfil `local`) - PADRÃO

Configurar as seguintes variáveis de ambiente apontando para os seus servidores:

```bash
# Configuração do PostgreSQL
export DATASOURCE_URL="jdbc:postgresql://seu-postgres-empresarial:5432/suadb"
export DATASOURCE_USERNAME="seuusuario"
export DATASOURCE_PASSWORD="suasenha"

# Configuração do Kafka
export KAFKA_BOOTSTRAP_SERVERS="seu-kafka-empresarial:9092"

# O perfil 'local' é ativado automaticamente (padrão)
# Para explicitamente definir: export SPRING_PROFILES_ACTIVE="local"
```

#### Opção B: Ambiente Local com Docker (perfil `docker`)

```bash
# Primeiro, iniciar o Docker Compose
docker-compose up -d
```

Isto inicia:
- **Kafka** (porta 9092) - modo KRaft, sem Zookeeper
- **PostgreSQL** (porta 5432) - banco de dados para ambas as aplicações
- **Prometheus** (porta 9090) - coleta de métricas
- **Grafana** (porta 3000) - visualização de métricas (admin/admin)

**Criar tópico de snapshots (opcional, será criado automaticamente):**
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic task-snapshots --partitions 3 --replication-factor 1 \
  --config cleanup.policy=compact
```

**Para usar este perfil, defina:**
```bash
export SPRING_PROFILES_ACTIVE="docker"
```

#### Opção C: Ambiente com Oracle Database (perfil `oracle`)

**1. Executar o script SQL de setup do Oracle:**
```sql
-- Execute o script em: producer-app/src/main/resources/oracle-outbox-setup.sql
-- Este script cria:
-- - Tabela OUTBOX_MESSAGES
-- - Sequence OUTBOX_SEQ
-- - Índices de performance
-- - (Opcional) Oracle AQ queue para integração JMS
```

**2. Configurar variáveis de ambiente:**
```bash
# Configuração do Oracle Database
export ORACLE_DATASOURCE_URL="jdbc:oracle:thin:@seu-oracle:1521:ORCL"
export ORACLE_DATASOURCE_USERNAME="seuusuario"
export ORACLE_DATASOURCE_PASSWORD="suasenha"

# Configuração do Kafka
export KAFKA_BOOTSTRAP_SERVERS="seu-kafka-empresarial:9092"

# (Opcional) Configuração do Oracle AQ
export ORACLE_AQ_QUEUE_NAME="OUTBOX_QUEUE"
export ORACLE_AQ_QUEUE_TABLE="OUTBOX_QUEUE_TABLE"
export ORACLE_AQ_POLL_INTERVAL_MS="1000"

# Ativar o perfil 'oracle'
export SPRING_PROFILES_ACTIVE="oracle"
```

**Notas sobre Oracle:**
- O outbox Oracle usa polling JDBC por padrão (similar ao PostgreSQL)
- Oracle AQ (Advanced Queuing) é opcional e pode ser configurado para integração JMS
- A tabela de outbox usa CLOB para payloads grandes
- Limpeza automática de mensagens antigas pode ser configurada (ver SQL script)


### 2. Build do Projeto

```bash
mvn clean install
```

### 3. Executar Producer

#### Com perfil Empresarial `local` (padrão):
```bash
cd producer-app
# Assumindo que as variáveis de ambiente já estão configuradas (ver seção 1)
mvn spring-boot:run
```

**Ou** com variáveis de ambiente inline:
```bash
cd producer-app
DATASOURCE_URL="jdbc:postgresql://seu-postgres:5432/suadb" \
DATASOURCE_USERNAME="seuusuario" \
DATASOURCE_PASSWORD="suasenha" \
KAFKA_BOOTSTRAP_SERVERS="seu-kafka:9092" \
mvn spring-boot:run
```

#### Com perfil Docker:
```bash
cd producer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker"
```

#### Com perfil Oracle:
```bash
cd producer-app
SPRING_PROFILES_ACTIVE=oracle \
ORACLE_DATASOURCE_URL="jdbc:oracle:thin:@seu-oracle:1521:ORCL" \
ORACLE_DATASOURCE_USERNAME="seuusuario" \
ORACLE_DATASOURCE_PASSWORD="suasenha" \
KAFKA_BOOTSTRAP_SERVERS="seu-kafka:9092" \
mvn spring-boot:run
```

O producer estará disponível em http://localhost:8080

### 4. Executar Consumer(s)

#### Com perfil Empresarial `local` (padrão):

**Terminal 1 (Consumer 1):**
```bash
cd consumer-app
# Assumindo que as variáveis de ambiente já estão configuradas
mvn spring-boot:run
```

**Terminal 2 (Consumer 2 - opcional):**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

**Terminal 3 (Consumer 3 - opcional):**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```

#### Com perfil Docker:

**Terminal 1 (Consumer 1):**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker"
```

**Terminal 2 (Consumer 2 - opcional):**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker --server.port=8082"
```

#### Com perfil Empresarial (variáveis inline):

```bash
cd consumer-app
DATASOURCE_URL="jdbc:postgresql://seu-postgres:5432/suadb" \
DATASOURCE_USERNAME="seuusuario" \
DATASOURCE_PASSWORD="suasenha" \
KAFKA_BOOTSTRAP_SERVERS="seu-kafka:9092" \
mvn spring-boot:run
```

## 🏗️ Arquitetura para Alto Volume

### Cenário: Milhares de Alterações por Task

Quando uma task sofre muitas alterações (ex: atualização massiva de atributos), sem agregação cada alteração geraria uma mensagem no Kafka, sobrecarregando o sistema e o frontend.

### Solução Implementada: Snapshot Aggregator

**Fluxo:**

```
[Producer] 
  ↓ insere outbox (atributo A mudou)
  ↓ insere outbox (atributo B mudou)
  ↓ insere outbox (atributo C mudou)
  ↓
[OutboxAggregatorService] (scheduled 500ms)
  ↓ agrupa por task_id
  ↓ aplica debounce (200ms)
  ↓ merge: última versão de cada atributo
  ↓ publica 1 snapshot completo → topic 'task-snapshots'
  ↓ marca mensagens originais como published
  ↓
[TaskSnapshotConsumer]
  ↓ consome snapshot
  ↓ atualiza task_snapshots (read-model)
  ↓ frontend lê versão completa
  ↓ (opcional) notifica frontend via WebSocket
```

### Configuração para Alto Débito

**Produtor:**
- `aggregator-interval-ms: 500` - Frequência de agregação
- `debounce-ms: 200` - Janela de espera antes de agregar
- Ajustar conforme volume (maior debounce = mais agregação, menor latência)

**Consumidor de Snapshots:**
- Usar tópico `task-snapshots` particionado por `taskId`
- Garantir ordenação por task (partition key)
- Consumer group dedicado (`task-snapshot-consumer-group`)
- Escalar consumidores conforme partições

**Kafka:**
- Criar tópico `task-snapshots` com número adequado de partições
- Configurar `cleanup.policy=compact` para reter apenas último snapshot por key
- Monitorizar consumer lag

### Alternativas Consideradas

1. **Mensagens por atributo + marcador final**: 
   - ❌ Complexo de implementar (changeSetId, seqNo, isLast)
   - ❌ Frontend precisa reconstruir estado
   
2. **Kafka Streams para agregação**:
   - ✅ Escalável e robusto
   - ❌ Mais complexo de configurar e manter
   
3. **Snapshot no Produtor** (escolhido):
   - ✅ Simples e eficaz
   - ✅ Menos mensagens no Kafka
   - ✅ Frontend consome estado completo
   - ⚠️ Debounce pode adicionar latência (200ms)

## 🔌 Integração com Oracle Database

O projeto suporta Oracle Database como alternativa ao PostgreSQL para a tabela de outbox, ideal para ambientes empresariais que já utilizam Oracle e/ou Oracle AQ (Advanced Queuing).

### Abordagens de Integração

#### 1. **Polling JDBC** (Implementação Atual - Recomendada)

A abordagem mais simples e compatível com todos os ambientes Oracle:

```
[Aplicação]
  ↓ insere transacionalmente em OUTBOX_MESSAGES (Oracle)
  ↓
[OracleOutboxPollingService] (scheduled 1s)
  ↓ consulta: SELECT * FROM OUTBOX_MESSAGES WHERE PUBLISHED = 0
  ↓ publica mensagens no Kafka
  ↓ atualiza: UPDATE OUTBOX_MESSAGES SET PUBLISHED = 1
```

**Vantagens:**
- ✅ Simples de implementar e manter
- ✅ Não requer configuração adicional do Oracle
- ✅ Funciona com qualquer versão do Oracle (12c+)
- ✅ Transacional e confiável

**Desvantagens:**
- ⚠️ Latência de polling (configurável, padrão 1s)
- ⚠️ Carga adicional no banco (queries periódicas)

#### 2. **Oracle AQ/JMS** (Disponível - Opcional)

Abordagem baseada em mensageria nativa do Oracle, usando Oracle Advanced Queuing:

```
[Aplicação]
  ↓ insere transacionalmente em OUTBOX_MESSAGES (Oracle)
  ↓ (trigger opcional) enfileira mensagem em AQ
  ↓
[Oracle AQ Queue: OUTBOX_QUEUE]
  ↓
[JMS Consumer] (na aplicação)
  ↓ recebe notificação instantânea da AQ
  ↓ publica no Kafka
  ↓ marca mensagem como publicada
```

**Vantagens:**
- ✅ Latência mínima (notificação push)
- ✅ Reduz carga de polling no banco
- ✅ Integração nativa com Oracle

**Desvantagens:**
- ❌ Requer Oracle AQ configurado e licenciado
- ❌ Maior complexidade de setup
- ❌ Dependências adicionais (Oracle AQ libraries)

**Setup Oracle AQ:**
```sql
-- Ver script completo em: producer-app/src/main/resources/oracle-outbox-setup.sql
BEGIN
    DBMS_AQADM.CREATE_QUEUE_TABLE(...);
    DBMS_AQADM.CREATE_QUEUE(...);
    DBMS_AQADM.START_QUEUE(...);
END;
```

#### 3. **Debezium com Oracle Connector** (Alternativa Externa)

Usar Debezium para capturar mudanças (CDC) na tabela de outbox Oracle:

```
[Oracle OUTBOX_MESSAGES]
  ↓
[Debezium Oracle Connector] (via LogMiner ou XStream)
  ↓ captura INSERTs via CDC
  ↓ publica diretamente no Kafka
  ↓
[Kafka Topic]
```

**Vantagens:**
- ✅ Desacoplado da aplicação
- ✅ Baixa latência
- ✅ Escalável

**Desvantagens:**
- ❌ Infraestrutura adicional (Kafka Connect)
- ❌ Requer permissões especiais no Oracle (LogMiner/XStream)
- ❌ Mais complexo de configurar

### Escolha da Abordagem

**Recomendação:** Usar **Polling JDBC** (implementação atual) por padrão.

- Se latência < 1s é crítica: considerar **Oracle AQ/JMS**
- Se preferir desacoplar do código: considerar **Debezium**

O projeto já implementa Polling JDBC e tem suporte básico para Oracle AQ (estruturas criadas no SQL script).

## 📊 Como Funciona

### Padrão Outbox (Producer)

1. Cliente faz POST para `/api/publish` ou `/api/publish-batch`
2. Mensagem é **inserida na tabela `outbox_messages`** (transacional)
3. `OutboxPollingService` (agendado a cada 1s) lê mensagens não publicadas
4. Publica no Kafka e marca como `published = true`
5. Usa `messageKey` para distribuir por partições

### Agregação de Outbox por Task (Snapshot Pattern)

Para lidar com alto volume de mensagens por task (ex: múltiplas alterações de atributos), 
o sistema implementa um padrão de agregação:

1. **OutboxAggregatorService** (agendado a cada 500ms) agrupa mensagens outbox por `task_id`
2. Aplica uma janela de **debounce** (200ms por padrão) para aguardar mensagens relacionadas
3. **Merge** de atributos: última alteração de cada atributo prevalece
4. Publica um **snapshot completo** da task no tópico `task-snapshots`
5. Marca mensagens originais como publicadas

**Benefícios:**
- Reduz drasticamente o número de mensagens enviadas ao Kafka
- Frontend consome apenas snapshots completos (simplifica lógica)
- Mantém ordenação por task (via partition key = taskId)
- Garante atomicidade (transacional)

### Consumer com Persistência

1. Recebe mensagem do Kafka (`@KafkaListener`)
2. Cria `MessageRecord` com `receivedAt` timestamp
3. **Simula processamento** (delay 2-20 segundos aleatório)
4. Tenta fazer parse como estrutura `Task` e persiste hierarquia
5. Atualiza `MessageRecord` com `processedAt` e `processingDurationMs`
6. **Commit manual** do offset apenas após persistência bem-sucedida

### Consumer de Snapshots (Read-Model)

1. **TaskSnapshotConsumer** consome do tópico `task-snapshots`
2. Atualiza tabela `task_snapshots` (read-model materializado)
3. Cada task tem um único registo com a versão mais recente
4. Frontend consulta `task_snapshots` para obter estado completo
5. Notificação pode ser enviada via WebSocket após atualização (future work)

### Evitar Rebalances

Configuração em `consumer-app/application.yml`:

```yaml
max.poll.interval.ms: 300000      # 5 minutos - tempo máximo entre polls
session.timeout.ms: 60000          # 1 minuto - tempo de sessão
heartbeat.interval.ms: 20000       # 20 segundos - intervalo de heartbeat
max.poll.records: 1                # 1 mensagem por poll (controle fino)
```

## 🧪 Testes de Integração

Execute os testes:

```bash
mvn test
```

Os testes usam:
- **Testcontainers** para PostgreSQL e Kafka
- **@EmbeddedKafka** para testes com Kafka
- **Awaitility** para assertions assíncronas

### Testes do Consumer

- Consumo de mensagem única
- Múltiplas mensagens com keys diferentes
- Parsing de estrutura Task hierárquica
- Verificação de timestamps e duração

### Testes do Producer

- Publicação via outbox pattern
- Distribuição por partições
- Múltiplas mensagens com diferentes clientes

## 📡 Endpoints API

### Producer App (porta 8080)

#### Publicar mensagem única
```bash
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello Kafka!",
    "partitionKey": "client-1"
  }'
```

#### Publicar lote de mensagens
```bash
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{
    "count": 30,
    "prefix": "TestMessage"
  }'
```

#### Estatísticas do Outbox
```bash
curl http://localhost:8080/api/outbox/stats
```

#### Health check
```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

### Consumer App (porta 8081+)

#### Parar consumo (graceful shutdown)
```bash
curl -X POST http://localhost:8081/internal/stop-consuming
```

#### Métricas Prometheus
```bash
curl http://localhost:8081/actuator/prometheus
```

## 📈 Monitorização

### Prometheus

Aceda a http://localhost:9090

Queries úteis:
```promql
# Taxa de mensagens processadas por segundo
rate(kafka_consumer_fetch_manager_records_consumed_total[1m])

# Duração média de processamento
avg(kafka_consumer_processing_duration_ms)

# Mensagens no outbox não publicadas
outbox_messages_unpublished_total
```

### Grafana

1. Aceda a http://localhost:3000 (admin/admin)
2. O datasource Prometheus já está configurado
3. Crie dashboards personalizados ou importe templates

Métricas expostas:
- `outbox.messages.published` - Total de mensagens publicadas
- `outbox.messages.failed` - Total de falhas na publicação
- Métricas padrão do Kafka (consumer lag, throughput, etc.)
- Métricas da aplicação (JVM, CPU, memória)

## 🗄️ Estrutura da Base de Dados

### Tabela: `tasks`
```sql
- id (bigserial)
- task_id (varchar, unique)
- raw_payload (text)
- created_at (timestamptz)
```

### Tabela: `task_attributes`
```sql
- id (bigserial)
- task_id (bigint FK)
- attribute_name (varchar)
- attribute_type (varchar) -- STRING, NUMERIC, DATE, BOOLEAN, ENTITY, TEXT
```

### Tabela: `task_attribute_values`
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

### Tabela: `message_records`
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

### Tabela: `outbox_messages` (PostgreSQL)
```sql
- id (bigserial)
- payload (text)
- message_key (varchar)
- topic (varchar)
- published (boolean)
- created_at (timestamptz)
- published_at (timestamptz)
- client_id (varchar)
- task_id (varchar)          -- NEW: usado para agregação por task
```

### Tabela: `OUTBOX_MESSAGES` (Oracle)
```sql
- ID (NUMBER(19))            -- Primary key com OUTBOX_SEQ
- PAYLOAD (CLOB)             -- JSON payload
- MESSAGE_KEY (VARCHAR2(500))
- TOPIC (VARCHAR2(255))
- PUBLISHED (NUMBER(1))      -- 0=false, 1=true
- CREATED_AT (TIMESTAMP WITH TIME ZONE)
- PUBLISHED_AT (TIMESTAMP WITH TIME ZONE)
- CLIENT_ID (VARCHAR2(255))
- TASK_ID (VARCHAR2(255))    -- Usado para agregação por task
```

**Nota:** Para setup completo do Oracle, execute o script:
`producer-app/src/main/resources/oracle-outbox-setup.sql`

### Tabela: `task_snapshots`
```sql
- id (bigserial)
- task_id (varchar, unique)  -- Identificador único da task
- snapshot_data (text)       -- JSON completo do snapshot
- version (bigint)           -- Versão do snapshot (incrementa a cada update)
- created_at (timestamptz)   -- Quando foi criado
- updated_at (timestamptz)   -- Última atualização
- kafka_offset (bigint)      -- Offset do Kafka de origem
- kafka_partition (integer)  -- Partição do Kafka
```

## 🎭 Cenários de Teste

### Teste 1: Distribuição Básica
1. Iniciar 1 consumer
2. Publicar 30 mensagens: `POST /api/publish-batch` com `count: 30`
3. Observar que o consumer processa de todas as 3 partições
4. Verificar logs para ver duração de processamento (2-20s por mensagem)

### Teste 2: Rebalanceamento
1. Iniciar 1 consumer (porta 8081)
2. Publicar mensagens
3. Iniciar 2º consumer (porta 8082) → observar rebalance nos logs
4. Publicar mais mensagens → distribuídas entre consumers
5. Parar 2º consumer → observar rebalance novamente

### Teste 3: Outbox Pattern em Tempo Real
1. Inserir mensagens diretamente na tabela outbox:
```sql
INSERT INTO outbox_messages (payload, message_key, topic, client_id, published, created_at)
VALUES ('Manual message', 'client-1', 'task-topic', 'client-1', false, NOW());
```
2. Observar mensagem ser publicada automaticamente (em 1s)
3. Verificar consumer processa a mensagem

### Teste 4: Processamento com Estrutura Task
```bash
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{
    "message": "{\"taskId\":\"TASK-001\",\"attributes\":[{\"name\":\"priority\",\"type\":\"STRING\",\"values\":[\"HIGH\"]},{\"name\":\"amount\",\"type\":\"NUMERIC\",\"values\":[\"1500.50\"]}]}",
    "partitionKey": "client-1"
  }'
```

Verificar na BD que a estrutura foi parseada e persistida:
```sql
SELECT t.task_id, ta.attribute_name, ta.attribute_type, 
       tav.string_value, tav.numeric_value
FROM tasks t
JOIN task_attributes ta ON ta.task_id = t.id
JOIN task_attribute_values tav ON tav.attribute_id = ta.id
WHERE t.task_id = 'TASK-001';
```

## 🔧 Configurações Importantes

### Perfis de Execução

O sistema suporta três perfis através da variável `SPRING_PROFILES_ACTIVE`:

- **`local`** (padrão): Usa Kafka e PostgreSQL externos via variáveis de ambiente (ambiente empresarial sem Docker)
- **`docker`**: Usa Kafka e PostgreSQL locais (localhost) via docker-compose
- **`oracle`**: Usa Oracle Database para outbox com Kafka externo via variáveis de ambiente

### Variáveis de Ambiente

#### Perfil `local` (PostgreSQL empresarial)

```bash
# PostgreSQL
DATASOURCE_URL=jdbc:postgresql://host:port/database
DATASOURCE_USERNAME=usuario
DATASOURCE_PASSWORD=senha

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092

# Perfil ativo (opcional, já é o padrão)
SPRING_PROFILES_ACTIVE=local
```

#### Perfil `docker` (Docker local)

```bash
# Não requer variáveis de ambiente - usa valores hardcoded em application-docker.yml
# Para ativar:
SPRING_PROFILES_ACTIVE=docker
```

#### Perfil `oracle` (Oracle Database)

```bash
# Oracle Database
ORACLE_DATASOURCE_URL=jdbc:oracle:thin:@host:port:SID
ORACLE_DATASOURCE_USERNAME=usuario
ORACLE_DATASOURCE_PASSWORD=senha

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092

# Oracle AQ (opcional)
ORACLE_AQ_QUEUE_NAME=OUTBOX_QUEUE
ORACLE_AQ_QUEUE_TABLE=OUTBOX_QUEUE_TABLE
ORACLE_AQ_POLL_INTERVAL_MS=1000

# Perfil ativo
SPRING_PROFILES_ACTIVE=oracle
```

### Como Alternar Entre Perfis

**Opção 1: Variável de ambiente**
```bash
export SPRING_PROFILES_ACTIVE=docker  # ou local, ou oracle
mvn spring-boot:run
```

**Opção 2: Argumento da linha de comando**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker"
```

**Opção 3: Propriedade do sistema**
```bash
mvn spring-boot:run -Dspring.profiles.active=docker
```

### Configurações do Consumer (application.yml)

```yaml
spring.kafka.consumer:
  max-poll-records: 1                    # Processar 1 msg de cada vez
  properties:
    max.poll.interval.ms: 300000         # 5 min - ajuste conforme necessário
    session.timeout.ms: 60000
    heartbeat.interval.ms: 20000

app.processing:
  min-delay-seconds: 2                   # Delay mínimo (ajustável)
  max-delay-seconds: 20                  # Delay máximo (ajustável)
```

### Configurações do Producer (application.yml)

```yaml
app.outbox:
  poll-interval-ms: 1000                 # Poll a cada 1 segundo
  batch-size: 100                        # Processar até 100 msgs por vez
  aggregator-interval-ms: 500            # Intervalo do agregador
  debounce-ms: 200                       # Janela de debounce para agregação
  
app.kafka:
  topic: task-topic                      # Tópico principal
  snapshot-topic: task-snapshots         # Tópico de snapshots agregados
```

## 🐳 Deployment em Kubernetes

Exemplo de Deployment com graceful shutdown:

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

## 📚 Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 3.1.5**
- **Spring Kafka** (não Spring Cloud Stream)
- **Hibernate 6.2.13** (Jakarta Persistence API)
- **PostgreSQL 15** (para perfil local/docker)
- **Oracle Database 12c+** (para perfil oracle - opcional)
- **Kafka 7.5.0** (modo KRaft, sem Zookeeper)
- **Prometheus + Grafana**
- **Testcontainers 1.19.1**
- **Maven**

## 🤔 Troubleshooting

### Kafka não arranca no Docker
```bash
docker-compose logs kafka
# Verificar se a porta 9092 está livre
# Recriar o volume se necessário: docker-compose down -v
```

### Perfil não está sendo aplicado corretamente
```bash
# Verificar qual perfil está ativo nos logs de inicialização:
# Procurar por: "The following profiles are active: local"

# Forçar perfil específico:
export SPRING_PROFILES_ACTIVE=docker  # ou local, ou oracle
mvn spring-boot:run

# Verificar configuração carregada:
curl http://localhost:8080/actuator/env | jq '.propertySources'
```

### Oracle: Erro de conexão
```bash
# Verificar URL do JDBC:
# Formato thin: jdbc:oracle:thin:@hostname:port:SID
# Formato service: jdbc:oracle:thin:@hostname:port/service_name
# TNS: jdbc:oracle:thin:@(DESCRIPTION=(...))

# Testar conectividade:
telnet seu-oracle-host 1521

# Verificar se o usuário tem permissões:
# - SELECT, INSERT, UPDATE, DELETE em OUTBOX_MESSAGES
# - SELECT em OUTBOX_SEQ
# - (Opcional) EXECUTE em DBMS_AQ, DBMS_AQADM para Oracle AQ
```

### Oracle: Tabela OUTBOX_MESSAGES não encontrada
```bash
# Executar o script de setup:
sqlplus usuario/senha@SID @producer-app/src/main/resources/oracle-outbox-setup.sql

# Verificar se a tabela foi criada:
sqlplus usuario/senha@SID
SQL> SELECT table_name FROM user_tables WHERE table_name = 'OUTBOX_MESSAGES';
SQL> SELECT sequence_name FROM user_sequences WHERE sequence_name = 'OUTBOX_SEQ';
```

### Oracle: Mensagens não estão sendo publicadas
```bash
# Verificar mensagens pendentes no outbox:
sqlplus usuario/senha@SID
SQL> SELECT COUNT(*) FROM OUTBOX_MESSAGES WHERE PUBLISHED = 0;

# Verificar logs do producer:
# Procurar por: "Processing N unpublished messages from Oracle outbox"

# Verificar se o serviço Oracle está ativo:
# Procurar por: "OracleOutboxPollingService" nos logs
# Se não aparecer, verificar se app.outbox.use-oracle=true no perfil
```

### Rebalances frequentes
- Aumentar `max.poll.interval.ms` se mensagens demoram muito
- Reduzir `max-poll-records` para processar menos mensagens por vez
- Verificar se consumers estão a fazer commit regularmente

### Mensagens não são consumidas
```bash
# Verificar offset do consumer group
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group task-consumer-group

# Verificar tópico
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic task-topic
```

### Outbox messages não são publicadas
```sql
-- Verificar mensagens pendentes
SELECT * FROM outbox_messages WHERE published = false;

-- Verificar logs do producer
# Logs devem mostrar "Publishing message X to topic Y"
```

## 📄 Licença

MIT License

## 👥 Contribuidores

Desenvolvido como PoC para demonstrar:
- Kafka moderno sem Zookeeper
- Padrão Outbox transacional
- Prevenção de rebalances em processamento longo
- Monitorização completa com Prometheus/Grafana
