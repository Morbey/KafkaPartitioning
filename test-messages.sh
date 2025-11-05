#!/bin/bash

# Script to send test messages to the producer

PRODUCER_URL="${PRODUCER_URL:-http://localhost:8080}"

echo "========================================="
echo "Kafka Message Test Script"
echo "========================================="
echo ""

# Function to send a single message
send_message() {
    local message=$1
    local key=$2
    
    echo "Sending message: '$message' with key: '$key'"
    curl -X POST "$PRODUCER_URL/api/publish" \
         -H "Content-Type: application/json" \
         -d "{\"message\":\"$message\",\"partitionKey\":\"$key\"}" \
         -w "\n" \
         2>/dev/null
    echo ""
}

# Function to send a batch
send_batch() {
    local count=$1
    local prefix=$2
    
    echo "Sending batch of $count messages with prefix '$prefix'"
    curl -X POST "$PRODUCER_URL/api/publish-batch" \
         -H "Content-Type: application/json" \
         -d "{\"count\":$count,\"prefix\":\"$prefix\"}" \
         -w "\n" \
         2>/dev/null
    echo ""
}

# Function to send structured task message
send_task() {
    local task_id=$1
    local client=$2
    
    local task_json=$(cat <<EOF
{
  "taskId": "$task_id",
  "attributes": [
    {
      "name": "priority",
      "type": "STRING",
      "values": ["HIGH"]
    },
    {
      "name": "amount",
      "type": "NUMERIC",
      "values": ["1500.50"]
    },
    {
      "name": "dueDate",
      "type": "DATE",
      "values": ["2025-12-31T23:59:59Z"]
    },
    {
      "name": "completed",
      "type": "BOOLEAN",
      "values": [false]
    }
  ]
}
EOF
)
    
    echo "Sending structured task: $task_id for client: $client"
    curl -X POST "$PRODUCER_URL/api/publish" \
         -H "Content-Type: application/json" \
         -d "{\"message\":$(echo $task_json | jq -c .),\"partitionKey\":\"$client\"}" \
         -w "\n" \
         2>/dev/null
    echo ""
}

# Main menu
while true; do
    echo ""
    echo "========================================="
    echo "Select test scenario:"
    echo "1. Send single message"
    echo "2. Send batch of messages (30)"
    echo "3. Send messages to different clients (9 messages, 3 clients)"
    echo "4. Send structured Task message"
    echo "5. Send large batch (100 messages)"
    echo "6. View outbox stats"
    echo "0. Exit"
    echo "========================================="
    read -p "Enter your choice: " choice
    
    case $choice in
        1)
            send_message "Test message $(date +%s)" "client-1"
            ;;
        2)
            send_batch 30 "TestMsg"
            ;;
        3)
            echo "Sending 9 messages distributed across 3 clients..."
            for i in {1..9}; do
                client_id=$((i % 3))
                send_message "Message-$i for client-$client_id" "client-$client_id"
                sleep 0.5
            done
            ;;
        4)
            send_task "TASK-$(date +%s)" "client-1"
            ;;
        5)
            send_batch 100 "LoadTest"
            ;;
        6)
            echo "Fetching outbox statistics..."
            curl "$PRODUCER_URL/api/outbox/stats" 2>/dev/null | jq .
            echo ""
            ;;
        0)
            echo "Exiting..."
            exit 0
            ;;
        *)
            echo "Invalid choice. Please try again."
            ;;
    esac
done
