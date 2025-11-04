package com.example.kafka.producer.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * OutboxMessage entity for transactional outbox pattern.
 * Messages are inserted here and then published to Kafka.
 */
@Entity
@Table(name = "outbox_messages", indexes = {
    @Index(name = "idx_published", columnList = "published,created_at"),
    @Index(name = "idx_message_key", columnList = "message_key")
})
public class OutboxMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;
    
    @Column(name = "message_key", nullable = false)
    private String messageKey;
    
    @Column(name = "topic", nullable = false)
    private String topic;
    
    @Column(name = "published", nullable = false)
    private Boolean published = false;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
    
    @Column(name = "client_id")
    private String clientId;
    
    public OutboxMessage() {
        this.createdAt = OffsetDateTime.now();
    }
    
    public OutboxMessage(String payload, String messageKey, String topic, String clientId) {
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
}
