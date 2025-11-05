package com.example.kafka.consumer.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Task Snapshot entity - materialized read-model for task state.
 * This table stores the latest complete state of each task,
 * updated by consuming task snapshot messages from Kafka.
 */
@Entity
@Table(name = "task_snapshots", indexes = {
    @Index(name = "idx_task_id", columnList = "task_id", unique = true),
    @Index(name = "idx_updated_at", columnList = "updated_at")
})
public class TaskSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "task_id", nullable = false, unique = true)
    private String taskId;
    
    @Column(name = "snapshot_data", columnDefinition = "TEXT", nullable = false)
    private String snapshotData;
    
    @Column(name = "version", nullable = false)
    private Long version = 1L;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    @Column(name = "kafka_offset")
    private Long kafkaOffset;
    
    @Column(name = "kafka_partition")
    private Integer kafkaPartition;
    
    public TaskSnapshot() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }
    
    public TaskSnapshot(String taskId, String snapshotData) {
        this();
        this.taskId = taskId;
        this.snapshotData = snapshotData;
    }
    
    public void updateSnapshot(String snapshotData) {
        this.snapshotData = snapshotData;
        this.updatedAt = OffsetDateTime.now();
        this.version++;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSnapshotData() {
        return snapshotData;
    }

    public void setSnapshotData(String snapshotData) {
        this.snapshotData = snapshotData;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getKafkaOffset() {
        return kafkaOffset;
    }

    public void setKafkaOffset(Long kafkaOffset) {
        this.kafkaOffset = kafkaOffset;
    }

    public Integer getKafkaPartition() {
        return kafkaPartition;
    }

    public void setKafkaPartition(Integer kafkaPartition) {
        this.kafkaPartition = kafkaPartition;
    }
}
