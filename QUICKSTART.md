# Guia Rápido - Kafka Partitions PoC

## 1. Pré-requisitos
- Java 17 instalado
- Maven instalado
- Docker e Docker Compose (para Kafka)

## 2. Iniciar Kafka
```bash
docker-compose up -d
```

Aguarde ~30 segundos para que Kafka esteja pronto.

## 3. Build do Projeto
```bash
mvn clean install
```

## 4. Executar Producer
```bash
cd producer-app
mvn spring-boot:run
```

Aguarde a mensagem: `Started ProducerApplication`

## 5. Executar Consumer (Múltiplas Instâncias)

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

## 6. Publicar Mensagens

Publicar 30 mensagens para testar:
```bash
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 30, "prefix": "Teste"}'
```

Publicar uma mensagem específica:
```bash
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{"message": "Olá Kafka!", "partitionKey": "key-1"}'
```

## 7. Observar os Logs

Nos logs dos consumers, procure por:
- `Received message: ...` - mensagem recebida
- `Partition: X` - de qual partição veio
- `ConsumerCoordinator` - informações de rebalanceamento

## 8. Testar Rebalanceamento

1. Inicie 1 consumer → ele recebe de todas as partições (0, 1, 2)
2. Publique mensagens
3. Inicie 2º consumer → observe o rebalanceamento nos logs
4. As partições serão redistribuídas entre os 2 consumers
5. Pare um consumer → as partições serão redistribuídas novamente

## 9. Parar Kafka
```bash
docker-compose down
```

## URLs Úteis
- Producer API: http://localhost:8080/api/publish
- Producer Health: http://localhost:8080/actuator/health
- Consumer 1 Health: http://localhost:8081/actuator/health
- Consumer 2 Health: http://localhost:8082/actuator/health
- Consumer 3 Health: http://localhost:8083/actuator/health
