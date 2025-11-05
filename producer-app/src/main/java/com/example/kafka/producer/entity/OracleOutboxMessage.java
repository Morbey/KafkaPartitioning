package com.example.kafka.producer.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Oracle-specific OutboxMessage entity for transactional outbox pattern.
 * Messages are inserted here and then published to Kafka via Oracle AQ/JMS.
 */
@Entity
@Table(name = "OUTBOX_MESSAGES", indexes = {
    @Index(name = "IDX_OUTBOX_PUBLISHED", columnList = "PUBLISHED,CREATED_AT"),
    @Index(name = "IDX_OUTBOX_MSG_KEY", columnList = "MESSAGE_KEY"),
    @Index(name = "IDX_OUTBOX_TASK_ID", columnList = "TASK_ID,PUBLISHED,CREATED_AT")
})
public class OracleOutboxMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbox_seq")
    @SequenceGenerator(name = "outbox_seq", sequenceName = "OUTBOX_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "PAYLOAD", columnDefinition = "CLOB", nullable = false)
    private String payload;
    
    @Column(name = "MESSAGE_KEY", nullable = false, length = 500)
    private String messageKey;
    
    @Column(name = "TOPIC", nullable = false, length = 255)
    private String topic;
    
    @Column(name = "PUBLISHED", nullable = false)
    private Boolean published = false;
    
    @Column(name = "CREATED_AT", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "PUBLISHED_AT")
    private OffsetDateTime publishedAt;
    
    @Column(name = "CLIENT_ID", length = 255)
    private String clientId;
    
    @Column(name = "TASK_ID", length = 255)
    private String taskId;
    
    public OracleOutboxMessage() {
        this.createdAt = OffsetDateTime.now();
    }
    
    public OracleOutboxMessage(String payload, String messageKey, String topic, String clientId) {
        this();
        this.payload = payload;
        this.messageKey = messageKey;
        this.topic = topic;
        this.clientId = clientId;
    }
    
    public void markPublished() {
        this.published = true;
        this.publishedAt = OffsetDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public String getMessageKey() {
        return messageKey;
    }
    
    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public Boolean getPublished() {
        return published;
    }
    
    public void setPublished(Boolean published) {
        this.published = published;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
