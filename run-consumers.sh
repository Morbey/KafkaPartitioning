#!/bin/bash

# Script para executar múltiplas instâncias do consumer
# Uso: ./run-consumers.sh [número de instâncias] [profile]
#   número de instâncias: 1-10 (default: 3)
#   profile: docker (default) or local

# Número de instâncias (default: 3)
NUM_INSTANCES=${1:-3}
PROFILE=${2:-docker}

# Porta base (8081, 8082, 8083, ...)
BASE_PORT=8081

# Diretório do consumer
CONSUMER_DIR="consumer-app"

echo "================================================"
echo "  Kafka Consumer - Múltiplas Instâncias"
echo "  Profile: $PROFILE"
echo "================================================"
echo ""
echo "Iniciando $NUM_INSTANCES instâncias do consumer..."
echo "Portas: $BASE_PORT até $((BASE_PORT + NUM_INSTANCES - 1))"
echo ""
echo "Pressione Ctrl+C para parar todas as instâncias"
echo "================================================"
echo ""

# Array para guardar PIDs
PIDS=()

# Função para limpar processos ao sair
cleanup() {
    echo ""
    echo "================================================"
    echo "  Parando todas as instâncias..."
    echo "================================================"
    for pid in "${PIDS[@]}"; do
        if ps -p $pid > /dev/null 2>&1; then
            echo "Parando processo $pid..."
            kill $pid 2>/dev/null
        fi
    done
    echo "Todas as instâncias foram paradas."
    exit 0
}

# Registrar função de cleanup para Ctrl+C
trap cleanup SIGINT SIGTERM

# Verificar se o diretório do consumer existe
if [ ! -d "$CONSUMER_DIR" ]; then
    echo "ERRO: Diretório $CONSUMER_DIR não encontrado!"
    echo "Execute este script a partir da raiz do projeto kafkaPartitionsPoc"
    exit 1
fi

# Verificar se o projeto foi compilado
if [ ! -f "$CONSUMER_DIR/target/consumer-app-0.0.1-SNAPSHOT.jar" ]; then
    echo "AVISO: JAR do consumer não encontrado."
    echo "Executando build primeiro..."
    mvn clean install -DskipTests
    if [ $? -ne 0 ]; then
        echo "ERRO: Build falhou!"
        exit 1
    fi
fi

# Iniciar as instâncias
for ((i=0; i<NUM_INSTANCES; i++)); do
    PORT=$((BASE_PORT + i))
    INSTANCE_ID=$((i + 1))
    
    echo "[$INSTANCE_ID] Iniciando consumer na porta $PORT com profile $PROFILE..."
    
    # Executar o JAR em background
    java -jar -Dspring.profiles.active=$PROFILE "$CONSUMER_DIR/target/consumer-app-0.0.1-SNAPSHOT.jar" \
        --server.port=$PORT \
        --spring.application.instance-id=consumer-$INSTANCE_ID \
        > "consumer-$INSTANCE_ID.log" 2>&1 &
    
    # Guardar o PID
    PIDS+=($!)
    
    echo "[$INSTANCE_ID] Iniciado com PID ${PIDS[$i]} (logs: consumer-$INSTANCE_ID.log)"
    
    # Pequeno delay entre instâncias
    sleep 2
done

echo ""
echo "================================================"
echo "  Todas as instâncias iniciadas!"
echo "  Profile: $PROFILE"
echo "================================================"
echo ""
echo "Instâncias em execução:"
for ((i=0; i<NUM_INSTANCES; i++)); do
    PORT=$((BASE_PORT + i))
    INSTANCE_ID=$((i + 1))
    echo "  [$INSTANCE_ID] PID: ${PIDS[$i]} | Porta: $PORT | Logs: consumer-$INSTANCE_ID.log"
done

echo ""
echo "Health checks:"
for ((i=0; i<NUM_INSTANCES; i++)); do
    PORT=$((BASE_PORT + i))
    echo "  http://localhost:$PORT/actuator/health"
done

echo ""
echo "Para ver os logs em tempo real, execute:"
echo "  tail -f consumer-1.log consumer-2.log consumer-3.log"
echo ""
echo "Aguardando... (Ctrl+C para parar)"
echo ""

# Aguardar indefinidamente
wait
