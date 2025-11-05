# Kafka Partitions PoC - Modern Setup without Zookeeper

Este projeto demonstra uma aplicação completa de Kafka usando Spring Kafka (sem Spring Cloud Stream), com persistência em PostgreSQL usando Hibernate 6, padrão Transactional Outbox, e monitorização com Prometheus e Grafana.

## 🎯 Características Principais

- ✅ **Kafka em modo KRaft** - Sem dependência de Zookeeper
- ✅ **Persistência completa** - PostgreSQL com Hibernate 6
- ✅ **Padrão Outbox** - Produção transacional de mensagens
- ✅ **Hierarquia de dados** - Task → TaskAttribute → TaskAttributeValue
- ✅ **Processamento simulado** - Delay configurável (2-20 segundos)
- ✅ **Prevenção de rebalances** - Configurações otimizadas para processamento longo
- ✅ **Graceful shutdown** - Endpoint para parar consumo antes de terminar o pod
- ✅ **Monitorização** - Prometheus + Grafana com métricas personalizadas
- ✅ **Testes de integração** - Testcontainers com Kafka e PostgreSQL
- ✅ **Distribuição por partições** - Mensagens distribuídas por key (cliente)

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

## ⚙️ Configuração de Ambientes

Este projeto suporta dois ambientes de execução. Para instruções detalhadas, consulte [CONFIGURACAO_AMBIENTES.md](CONFIGURACAO_AMBIENTES.md).

### 🏢 Ambiente Empresarial (Perfil `local`)
Para usar Kafka e PostgreSQL externos (sem Docker local):

1. **Editar configuração**: Abra `application-local.yaml` em ambas as aplicações (producer-app e consumer-app) e configure:
   - `spring.datasource.url`: URL do PostgreSQL empresarial
   - `spring.datasource.username`: Utilizador da BD
   - `spring.datasource.password`: Password da BD
   - `spring.kafka.bootstrap-servers`: Endereço do Kafka empresarial

2. **Executar com perfil local**:
   ```bash
   # Producer
   cd producer-app
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   
   # Consumer
   cd consumer-app
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

### 🐳 Ambiente Docker Local (Perfil `docker` - padrão)
Para usar Kafka e PostgreSQL em containers Docker:

1. **Iniciar infraestrutura**:
   ```bash
   docker-compose up -d
   ```

2. **Executar aplicações**:
   ```bash
   cd producer-app && mvn spring-boot:run
   cd consumer-app && mvn spring-boot:run
   ```

## 🚀 Quick Start

### Pré-requisitos

- Java 17+
- Maven 3.6+
- Docker e Docker Compose (apenas para ambiente Docker)

### 1. Iniciar Infraestrutura (Apenas para Ambiente Docker)

```bash
docker-compose up -d
```

Isto inicia:
- **Kafka** (porta 9092) - modo KRaft, sem Zookeeper
- **PostgreSQL** (porta 5432) - banco de dados para ambas as aplicações
- **Prometheus** (porta 9090) - coleta de métricas
- **Grafana** (porta 3000) - visualização de métricas (admin/admin)

### 2. Build do Projeto

```bash
mvn clean install
```

### 3. Executar Producer

```bash
cd producer-app
mvn spring-boot:run
```

O producer estará disponível em http://localhost:8080

### 4. Executar Consumer(s)

**Terminal 1 (Consumer 1):**
```bash
cd consumer-app
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

## 📊 Como Funciona

### Padrão Outbox (Producer)

1. Cliente faz POST para `/api/publish` ou `/api/publish-batch`
2. Mensagem é **inserida na tabela `outbox_messages`** (transacional)
3. `OutboxPollingService` (agendado a cada 1s) lê mensagens não publicadas
4. Publica no Kafka e marca como `published = true`
5. Usa `messageKey` para distribuir por partições

### Consumer com Persistência

1. Recebe mensagem do Kafka (`@KafkaListener`)
2. Cria `MessageRecord` com `receivedAt` timestamp
3. **Simula processamento** (delay 2-20 segundos aleatório)
4. Tenta fazer parse como estrutura `Task` e persiste hierarquia
5. Atualiza `MessageRecord` com `processedAt` e `processingDurationMs`
6. **Commit manual** do offset apenas após persistência bem-sucedida

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

### Tabela: `outbox_messages`
```sql
- id (bigserial)
- payload (text)
- message_key (varchar)
- topic (varchar)
- published (boolean)
- created_at (timestamptz)
- published_at (timestamptz)
- client_id (varchar)
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
- **PostgreSQL 15**
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
