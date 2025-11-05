package com.example.kafka.consumer.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TaskAttribute entity representing an attribute of a task.
 * One task attribute can have multiple values.
 */
@Entity
@Table(name = "task_attributes")
public class TaskAttribute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    
    @Column(name = "attribute_name", nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "attribute_type", nullable = false)
    private AttributeType type;
    
    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskAttributeValue> values = new ArrayList<>();
    
    public TaskAttribute() {
    }
    
    public TaskAttribute(String name, AttributeType type) {
        this.name = name;
        this.type = type;
    }
    
    // Helper method to add value
    public void addValue(TaskAttributeValue value) {
        values.add(value);
        value.setAttribute(this);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Task getTask() {
        return task;
    }
    
    public void setTask(Task task) {
        this.task = task;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public AttributeType getType() {
        return type;
    }
    
    public void setType(AttributeType type) {
        this.type = type;
    }
    
    public List<TaskAttributeValue> getValues() {
        return values;
    }
    
    public void setValues(List<TaskAttributeValue> values) {
        this.values = values;
    }
}
