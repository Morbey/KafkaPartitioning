# Kafka Partitions PoC

Este projeto contém mini-aplicações Maven para testar o funcionamento de partições Kafka usando Spring Cloud Stream e Java 17.

## Estrutura do Projeto

```
kafkaPartitionsPoc/
├── consumer-app/          # Aplicação consumidora Kafka
├── producer-app/          # Aplicação produtora Kafka com REST API
└── pom.xml               # POM parent
```

## Pré-requisitos

- Java 17
- Maven 3.6+
- Kafka (executando em localhost:9092)
- Docker (opcional, para executar Kafka)

## Executar Kafka com Docker

Se não tiver Kafka instalado localmente, pode usar Docker Compose:

```bash
# Criar docker-compose.yml (ver exemplo abaixo)
docker-compose up -d
```

Exemplo de `docker-compose.yml`:

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
```

## Build do Projeto

Compilar todos os módulos:

```bash
cd /caminho/para/kafkaPartitionsPoc
mvn clean install
```

## Executar as Aplicações

### 1. Producer App (Porta 8080)

```bash
cd producer-app
mvn spring-boot:run
```

Ou executar o JAR:

```bash
java -jar producer-app/target/producer-app-0.0.1-SNAPSHOT.jar
```

### 2. Consumer App - Múltiplas Instâncias

Para observar o rebalanceamento, execute **várias instâncias** do consumer em terminais diferentes:

**Terminal 1:**
```bash
cd consumer-app
mvn spring-boot:run
```

**Terminal 2:**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

**Terminal 3:**
```bash
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```

Ou usando JARs:

```bash
# Terminal 1
java -jar consumer-app/target/consumer-app-0.0.1-SNAPSHOT.jar --server.port=8081

# Terminal 2
java -jar consumer-app/target/consumer-app-0.0.1-SNAPSHOT.jar --server.port=8082

# Terminal 3
java -jar consumer-app/target/consumer-app-0.0.1-SNAPSHOT.jar --server.port=8083
```

## Testar o Particionamento

### Via REST API (Producer App)

O producer-app expõe endpoints REST para publicar mensagens:

#### Publicar uma mensagem única:

```bash
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Olá Kafka!",
    "partitionKey": "key-1"
  }'
```

#### Publicar um lote de mensagens:

```bash
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{
    "count": 30,
    "prefix": "Teste"
  }'
```

Este comando publica 30 mensagens distribuídas por 3 partições.

### Via Kafka Console Producer

Também pode usar as ferramentas do Kafka:

```bash
# Criar o tópico com 3 partições
kafka-topics.sh --create --topic test-topic --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092

# Publicar mensagens
kafka-console-producer.sh --broker-list localhost:9092 --topic test-topic
```

## Observar o Rebalanceamento

Quando você executa múltiplas instâncias do consumer:

1. **Inicialmente**, cada consumer será atribuído a diferentes partições
2. **Logs do consumer** mostrarão as partições atribuídas:
   ```
   ConsumerCoordinator : (Re-)joining group
   ConsumerCoordinator : Successfully joined group with generation X
   ConsumerCoordinator : Assigned partitions: [test-topic-0, test-topic-1]
   ```
3. **Quando adicionar/remover** uma instância, verá mensagens de rebalanceamento nos logs
4. As mensagens consumidas mostrarão a **partição e offset** de onde foram lidas

### O que observar nos logs:

- **Partition assignment**: Quais partições cada consumer está a processar
- **Rebalancing**: Quando consumers entram/saem do grupo
- **Message distribution**: Como as mensagens são distribuídas pelas partições

## Configuração de Partições

### Producer (producer-app/src/main/resources/application.yml)

```yaml
spring:
  cloud:
    stream:
      bindings:
        produceMessage-out-0:
          producer:
            partition-count: 3                              # Número de partições
            partition-key-expression: headers['partitionKey'] # Como distribuir
```

### Consumer (consumer-app/src/main/resources/application.yml)

```yaml
spring:
  cloud:
    stream:
      bindings:
        consumeMessage-in-0:
          group: test-consumer-group    # Mesmo grupo = rebalancing
          consumer:
            concurrency: 3               # Threads por instância
            partitioned: true
```

## Endpoints Úteis

### Producer App (porta 8080)
- `POST /api/publish` - Publicar uma mensagem
- `POST /api/publish-batch` - Publicar lote de mensagens
- `GET /api/health` - Health check
- `GET /actuator/health` - Actuator health

### Consumer App (porta 8081, 8082, 8083...)
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Métricas

## Testes Recomendados

### Teste 1: Distribuição Básica
1. Inicie 1 consumer
2. Publique 30 mensagens: `POST /api/publish-batch` com `count: 30`
3. Observe que o único consumer recebe de todas as 3 partições

### Teste 2: Rebalanceamento
1. Inicie 1 consumer (recebe de todas as partições)
2. Publique mensagens
3. Inicie um 2º consumer → observe o rebalanceamento nos logs
4. Publique mais mensagens → observe que são distribuídas entre os 2 consumers
5. Pare o 2º consumer → observe o rebalanceamento novamente

### Teste 3: Máximo de Consumers
1. Inicie 3 consumers (igual ao número de partições)
2. Cada consumer deve receber de 1 partição
3. Tente iniciar um 4º consumer → ele ficará idle (sem partições atribuídas)

## Troubleshooting

### Kafka não está a executar
```
Caused by: org.apache.kafka.common.errors.TimeoutException
```
**Solução**: Certifique-se que Kafka está a executar em localhost:9092

### Porta já em uso
```
Port 8080 is already in use
```
**Solução**: Use `--server.port=XXXX` para especificar outra porta

### Tópico não existe
O tópico `test-topic` é criado automaticamente com `auto-create-topics: true`. Se tiver problemas, crie manualmente:

```bash
kafka-topics.sh --create --topic test-topic --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092
```

## Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 3.1.5**
- **Spring Cloud 2022.0.4**
- **Spring Cloud Stream** (functional programming model)
- **Spring Cloud Stream Kafka Binder**
- **Maven**

## Abrir no IntelliJ

1. Abra o IntelliJ IDEA
2. File → Open
3. Selecione a pasta `kafkaPartitionsPoc` (o diretório com o pom.xml parent)
4. IntelliJ irá importar todos os módulos automaticamente
5. Configure múltiplas Run Configurations para o consumer-app com portas diferentes

## Notas Adicionais

- Todos os consumers no mesmo `group` participam no rebalanceamento
- O número máximo de consumers úteis = número de partições
- Mensagens com a mesma `partitionKey` vão sempre para a mesma partição
- O rebalanceamento acontece quando consumers entram ou saem do grupo
