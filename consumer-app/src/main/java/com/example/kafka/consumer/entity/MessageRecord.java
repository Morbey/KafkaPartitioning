package com.example.kafka.consumer.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * MessageRecord entity for storing consumed messages and metadata.
 * Tracks received time and processing completion time.
 */
@Entity
@Table(name = "message_records", indexes = {
    @Index(name = "idx_kafka_topic_partition", columnList = "kafka_topic,partition"),
    @Index(name = "idx_received_at", columnList = "received_at"),
    @Index(name = "idx_message_key", columnList = "message_key")
})
public class MessageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;
    
    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;
    
    @Column(name = "processed_at")
    private OffsetDateTime processedAt;
    
    @Column(name = "kafka_topic")
    private String kafkaTopic;
    
    @Column(name = "partition")
    private Integer partition;
    
    @Column(name = "offset_value")
    private Long offsetValue;
    
    @Column(name = "message_key")
    private String messageKey;
    
    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;
    
    public MessageRecord() {
    }
    
    public MessageRecord(String rawMessage, String kafkaTopic, Integer partition, Long offsetValue, String messageKey) {
        this.rawMessage = rawMessage;
        this.receivedAt = OffsetDateTime.now();
        this.kafkaTopic = kafkaTopic;
        this.partition = partition;
        this.offsetValue = offsetValue;
        this.messageKey = messageKey;
    }
    
    public void markProcessed(long processingDurationMs) {
        this.processedAt = OffsetDateTime.now();
        this.processingDurationMs = processingDurationMs;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRawMessage() {
        return rawMessage;
    }
    
    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }
    
    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }
    
    public void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
    
    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getKafkaTopic() {
        return kafkaTopic;
    }
    
    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }
    
    public Integer getPartition() {
        return partition;
    }
    
    public void setPartition(Integer partition) {
        this.partition = partition;
    }
    
    public Long getOffsetValue() {
        return offsetValue;
    }
    
    public void setOffsetValue(Long offsetValue) {
        this.offsetValue = offsetValue;
    }
    
    public String getMessageKey() {
        return messageKey;
    }
    
    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }
    
    public Long getProcessingDurationMs() {
        return processingDurationMs;
    }
    
    public void setProcessingDurationMs(Long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
    }
}
