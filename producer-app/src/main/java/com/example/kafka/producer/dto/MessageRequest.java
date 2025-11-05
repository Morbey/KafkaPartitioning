package com.example.kafka.producer.dto;

public class MessageRequest {
    private String message;
    private String partitionKey;

    public MessageRequest() {
    }

    public MessageRequest(String message, String partitionKey) {
        this.message = message;
        this.partitionKey = partitionKey;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }
}
