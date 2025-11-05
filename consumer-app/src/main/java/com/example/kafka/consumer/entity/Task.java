package com.example.kafka.consumer.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Task entity representing a hierarchical task structure.
 * One task can have multiple task attributes.
 */
@Entity
@Table(name = "tasks")
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "task_id", unique = true)
    private String taskId;
    
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskAttribute> attributes = new ArrayList<>();
    
    public Task() {
        this.createdAt = OffsetDateTime.now();
    }
    
    public Task(String taskId, String rawPayload) {
        this();
        this.taskId = taskId;
        this.rawPayload = rawPayload;
    }
    
    // Helper method to add attribute
    public void addAttribute(TaskAttribute attribute) {
        attributes.add(attribute);
        attribute.setTask(this);
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
    
    public String getRawPayload() {
        return rawPayload;
    }
    
    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<TaskAttribute> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(List<TaskAttribute> attributes) {
        this.attributes = attributes;
    }
}
