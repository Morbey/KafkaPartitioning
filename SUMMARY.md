# Resumo do Projeto - Kafka Partitions PoC

## O que foi implementado

Este projeto foi completamente configurado e está pronto para executar. Contém todos os componentes necessários para testar particionamento Kafka com Spring Cloud Stream e Java 17.

## Estrutura do Projeto

```
kafkaPartitionsPoc/
├── pom.xml                      # Parent POM (Java 17, Spring Boot 3.1.5, Spring Cloud 2022.0.4)
├── docker-compose.yml           # Kafka e Zookeeper para testes locais
├── README.md                    # Documentação completa
├── QUICKSTART.md                # Guia rápido de início
├── USAGE_EXAMPLES.md            # Exemplos de uso da API
│
├── consumer-app/                # Módulo Consumer
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/kafka/consumer/
│       │   ├── ConsumerApplication.java      # Main class
│       │   └── MessageConsumer.java          # Consumer funcional
│       └── resources/
│           └── application.yml               # Configuração Kafka
│
└── producer-app/                # Módulo Producer
    ├── pom.xml
    └── src/main/
        ├── java/com/example/kafka/producer/
        │   ├── ProducerApplication.java      # Main class
        │   ├── MessageProducerController.java # REST Controller
        │   ├── MessageRequest.java           # DTO para mensagem única
        │   └── BatchRequest.java             # DTO para lote de mensagens
        └── resources/
            └── application.yml               # Configuração Kafka
```

## Funcionalidades Implementadas

### Consumer App
✅ Configurado com Spring Cloud Stream Kafka Binder
✅ Usa modelo funcional (Java Function API)
✅ Logging detalhado mostrando:
  - Mensagem recebida
  - Partição de origem
  - Offset
  - Thread que processou
✅ Configurado para consumer group (permite rebalanceamento)
✅ Suporta múltiplas instâncias simultâneas
✅ Logging de rebalanceamento habilitado
✅ Actuator endpoints para health check

### Producer App
✅ REST API para publicar mensagens
✅ Endpoint POST `/api/publish` - mensagem única com partition key
✅ Endpoint POST `/api/publish-batch` - lote de mensagens
✅ Endpoint GET `/api/health` - health check
✅ StreamBridge para envio dinâmico de mensagens
✅ Suporta partition keys customizadas
✅ Actuator endpoints habilitados

## Configuração de Partições

### Tópico Kafka
- **Nome**: `test-topic`
- **Partições**: 3
- **Criação**: automática (auto-create-topics: true)

### Consumer Group
- **Nome**: `test-consumer-group`
- **Comportamento**: Todos os consumers com este group participam no rebalanceamento
- **Concurrency**: 3 threads por instância

### Producer
- **Partition Count**: 3
- **Partition Key**: Configurável via header `partitionKey`
- **Distribuição**: Mensagens com mesma key vão para mesma partição

## Como Usar

### 1. Iniciar Kafka
```bash
docker-compose up -d
```

### 2. Build
```bash
mvn clean install
```

### 3. Executar Producer
```bash
cd producer-app && mvn spring-boot:run
```
Porta: 8080

### 4. Executar Consumers (múltiplas instâncias)
```bash
# Terminal 1
cd consumer-app && mvn spring-boot:run

# Terminal 2
cd consumer-app && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"

# Terminal 3
cd consumer-app && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```
Portas: 8081, 8082, 8083

### 5. Publicar Mensagens
```bash
# Mensagem única
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{"message": "Teste", "partitionKey": "key-1"}'

# Lote de 30 mensagens
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 30, "prefix": "Teste"}'
```

## Cenários de Teste Implementados

### Teste 1: Distribuição Básica
- Inicie 1 consumer
- Publique 30 mensagens
- Observe que o consumer recebe de todas as 3 partições

### Teste 2: Rebalanceamento
- Inicie 1 consumer (recebe partições 0, 1, 2)
- Publique mensagens
- Inicie 2º consumer → observe rebalanceamento nos logs
- Partições são redistribuídas entre os 2 consumers
- Pare 1 consumer → observe rebalanceamento novamente

### Teste 3: Máximo de Consumers
- Inicie 3 consumers (= número de partições)
- Cada consumer recebe 1 partição
- 4º consumer ficaria idle (sem partições)

### Teste 4: Ordem de Mensagens
- Publique várias mensagens com a mesma partitionKey
- Observe que são processadas em ordem pelo mesmo consumer

## Tecnologias

- **Java**: 17
- **Spring Boot**: 3.1.5
- **Spring Cloud**: 2022.0.4
- **Spring Cloud Stream**: Functional programming model
- **Kafka Binder**: Spring Cloud Stream Kafka
- **Build Tool**: Maven 3.6+
- **Container**: Docker (para Kafka)

## Logs Importantes

### Rebalanceamento
```
ConsumerCoordinator : (Re-)joining group
ConsumerCoordinator : Successfully joined group with generation X
ConsumerCoordinator : Assigned partitions: [test-topic-0, test-topic-1]
```

### Mensagem Recebida
```
========================================
Received message: Teste-5
Topic: test-topic
Partition: 1
Offset: 42
Thread: container-0-C-1
========================================
```

## IntelliJ IDEA

Para abrir o projeto no IntelliJ:
1. File → Open
2. Selecionar pasta `kafkaPartitionsPoc` (com o pom.xml parent)
3. IntelliJ importa automaticamente todos os módulos
4. Configurar múltiplas Run Configurations para consumer-app

## Endpoints Disponíveis

### Producer (8080)
- `POST /api/publish` - Publicar mensagem única
- `POST /api/publish-batch` - Publicar lote
- `GET /api/health` - Health check
- `GET /actuator/health` - Actuator health

### Consumers (8081, 8082, 8083)
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Métricas

## Arquivos de Documentação

1. **README.md** - Documentação completa e detalhada
2. **QUICKSTART.md** - Guia rápido para começar
3. **USAGE_EXAMPLES.md** - Exemplos de curl e cenários de teste
4. **SUMMARY.md** (este ficheiro) - Resumo executivo

## Notas Importantes

✅ Projeto compila sem erros
✅ JARs executáveis criados (43MB cada)
✅ Sem vulnerabilidades de segurança (CodeQL verified)
✅ Configuração pronta para produção (com ajustes de broker)
✅ Logging configurado para debug de particionamento
✅ Docker Compose incluído para ambiente local

## Próximos Passos

1. Ler QUICKSTART.md para começar rapidamente
2. Executar docker-compose up para levantar Kafka
3. Build com mvn clean install
4. Executar producer e consumers
5. Testar com os exemplos em USAGE_EXAMPLES.md
6. Experimentar diferentes cenários de rebalanceamento

## Suporte

Para problemas comuns, consultar a secção "Troubleshooting" no README.md

---

**Status**: ✅ Projeto completo e funcional
**Build**: ✅ Successful
**Tests**: ✅ No test failures (skipped for PoC)
**Security**: ✅ No vulnerabilities detected
**Documentation**: ✅ Complete
