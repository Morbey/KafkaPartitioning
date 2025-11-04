# Exemplos de Uso - Producer API

Este ficheiro contém exemplos práticos de como usar a API do producer para testar particionamento Kafka.

## Verificar se Producer está a executar

```bash
curl http://localhost:8080/api/health
```

Resposta esperada:
```json
{
  "status": "UP",
  "service": "producer-app"
}
```

## Publicar Mensagem Individual

### Com Partition Key Específica

```bash
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Mensagem para partição 1",
    "partitionKey": "key-1"
  }'
```

### Sem Partition Key (distribuição aleatória)

```bash
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Mensagem aleatória"
  }'
```

### Diferentes Partition Keys (testar distribuição)

```bash
# Partição 0
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{"message": "Para partição 0", "partitionKey": "key-0"}'

# Partição 1
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{"message": "Para partição 1", "partitionKey": "key-1"}'

# Partição 2
curl -X POST http://localhost:8080/api/publish \
  -H "Content-Type: application/json" \
  -d '{"message": "Para partição 2", "partitionKey": "key-2"}'
```

## Publicar Lote de Mensagens

### Publicar 10 mensagens

```bash
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{
    "count": 10,
    "prefix": "Teste"
  }'
```

Isto publica 10 mensagens: Teste-0, Teste-1, ..., Teste-9
Distribuídas por 3 partições usando keys: key-0, key-1, key-2

### Publicar 30 mensagens (10 por partição)

```bash
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{
    "count": 30,
    "prefix": "Carga"
  }'
```

### Publicar 100 mensagens para stress test

```bash
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{
    "count": 100,
    "prefix": "StressTest"
  }'
```

## Cenários de Teste

### Cenário 1: Testar Distribuição Básica

1. Publique mensagens para cada partição:

```bash
for i in {0..2}; do
  curl -X POST http://localhost:8080/api/publish \
    -H "Content-Type: application/json" \
    -d "{\"message\": \"Mensagem partição $i\", \"partitionKey\": \"key-$i\"}"
  echo ""
done
```

2. Observe nos logs dos consumers qual partição processou cada mensagem.

### Cenário 2: Testar Rebalanceamento

1. Com 1 consumer a executar, publique 30 mensagens:

```bash
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 30, "prefix": "Pre-Rebalance"}'
```

2. Inicie 2º consumer

3. Publique mais 30 mensagens:

```bash
curl -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 30, "prefix": "Post-Rebalance"}'
```

4. Compare os logs - as mensagens devem estar distribuídas entre os 2 consumers

### Cenário 3: Testar Ordem nas Partições

Mensagens com a mesma key vão sempre para a mesma partição e são processadas em ordem:

```bash
# Publicar 5 mensagens com a mesma key
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/publish \
    -H "Content-Type: application/json" \
    -d "{\"message\": \"Ordem-$i\", \"partitionKey\": \"key-ordem\"}"
  echo ""
  sleep 0.5
done
```

Nos logs do consumer, deve ver Ordem-1, Ordem-2, ..., Ordem-5 na mesma ordem, todas da mesma partição.

### Cenário 4: Carga Contínua

Publicar mensagens continuamente para observar consumers:

```bash
# Loop infinito - use Ctrl+C para parar
while true; do
  curl -X POST http://localhost:8080/api/publish-batch \
    -H "Content-Type: application/json" \
    -d '{"count": 10, "prefix": "Continuous"}'
  echo "Published batch..."
  sleep 2
done
```

## Script de Teste Completo

Guardar isto num ficheiro `test-kafka.sh`:

```bash
#!/bin/bash

echo "=== Teste 1: Verificar Producer ==="
curl -s http://localhost:8080/api/health | jq
echo ""

echo "=== Teste 2: Publicar mensagens individuais ==="
for i in {0..2}; do
  echo "Publicando para partição $i..."
  curl -s -X POST http://localhost:8080/api/publish \
    -H "Content-Type: application/json" \
    -d "{\"message\": \"Partição $i\", \"partitionKey\": \"key-$i\"}" | jq
  sleep 1
done

echo ""
echo "=== Teste 3: Publicar lote ==="
curl -s -X POST http://localhost:8080/api/publish-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 30, "prefix": "Batch"}' | jq

echo ""
echo "=== Testes completos! Verifique os logs dos consumers ==="
```

Dar permissões de execução e executar:
```bash
chmod +x test-kafka.sh
./test-kafka.sh
```

## Monitorizar Consumers

Para verificar se os consumers estão activos:

```bash
# Consumer 1
curl -s http://localhost:8081/actuator/health | jq

# Consumer 2
curl -s http://localhost:8082/actuator/health | jq

# Consumer 3
curl -s http://localhost:8083/actuator/health | jq
```

## Dicas

1. **Use `jq`** para formatar o JSON da resposta (instale com `apt install jq` ou `brew install jq`)
2. **Verifique os logs** dos consumers para ver partition, offset e mensagem
3. **Experimente parar/iniciar** consumers enquanto publica mensagens
4. **Mensagens com mesmo partitionKey** vão sempre para a mesma partição
5. **Número de consumers úteis** = número de partições (3 neste caso)
