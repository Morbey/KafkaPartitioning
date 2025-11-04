#!/bin/bash

# Script para executar o producer
# Uso: ./run-producer.sh

PRODUCER_DIR="producer-app"
PORT=8080

echo "================================================"
echo "  Kafka Producer"
echo "================================================"
echo ""

# Função para limpar processos ao sair
cleanup() {
    echo ""
    echo "================================================"
    echo "  Parando producer..."
    echo "================================================"
    if [ ! -z "$PRODUCER_PID" ]; then
        kill $PRODUCER_PID 2>/dev/null
    fi
    echo "Producer parado."
    exit 0
}

# Registrar função de cleanup para Ctrl+C
trap cleanup SIGINT SIGTERM

# Verificar se o diretório do producer existe
if [ ! -d "$PRODUCER_DIR" ]; then
    echo "ERRO: Diretório $PRODUCER_DIR não encontrado!"
    echo "Execute este script a partir da raiz do projeto kafkaPartitionsPoc"
    exit 1
fi

# Verificar se o projeto foi compilado
if [ ! -f "$PRODUCER_DIR/target/producer-app-0.0.1-SNAPSHOT.jar" ]; then
    echo "AVISO: JAR do producer não encontrado."
    echo "Executando build primeiro..."
    mvn clean install -DskipTests
    if [ $? -ne 0 ]; then
        echo "ERRO: Build falhou!"
        exit 1
    fi
fi

echo "Iniciando producer na porta $PORT..."
echo ""

# Executar o JAR
java -jar "$PRODUCER_DIR/target/producer-app-0.0.1-SNAPSHOT.jar" \
    --server.port=$PORT \
    > producer.log 2>&1 &

PRODUCER_PID=$!

echo "Producer iniciado com PID $PRODUCER_PID"
echo "Logs: producer.log"
echo ""
echo "================================================"
echo "  Endpoints disponíveis:"
echo "================================================"
echo "  POST http://localhost:$PORT/api/publish"
echo "  POST http://localhost:$PORT/api/publish-batch"
echo "  GET  http://localhost:$PORT/api/health"
echo "  GET  http://localhost:$PORT/actuator/health"
echo ""
echo "Exemplos de uso:"
echo ""
echo "  # Publicar mensagem única"
echo "  curl -X POST http://localhost:$PORT/api/publish \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"message\": \"Teste\", \"partitionKey\": \"key-1\"}'"
echo ""
echo "  # Publicar lote de 30 mensagens"
echo "  curl -X POST http://localhost:$PORT/api/publish-batch \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"count\": 30, \"prefix\": \"Teste\"}'"
echo ""
echo "Para ver os logs em tempo real:"
echo "  tail -f producer.log"
echo ""
echo "Aguardando... (Ctrl+C para parar)"
echo ""

# Aguardar
wait $PRODUCER_PID
