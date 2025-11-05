package com.example.kafka.consumer.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * TaskAttributeValue entity representing a value of a task attribute.
 * Uses polymorphic storage - different columns for different types.
 */
@Entity
@Table(name = "task_attribute_values")
public class TaskAttributeValue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private TaskAttribute attribute;
    
    // Different value columns based on type
    @Column(name = "string_value")
    private String stringValue;
    
    @Column(name = "numeric_value", precision = 19, scale = 4)
    private BigDecimal numericValue;
    
    @Column(name = "date_value")
    private OffsetDateTime dateValue;
    
    @Column(name = "boolean_value")
    private Boolean booleanValue;
    
    @Column(name = "entity_ref")
    private String entityRef;
    
    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;
    
    public TaskAttributeValue() {
    }
    
    // Static factory methods for different types
    public static TaskAttributeValue ofString(String value) {
        TaskAttributeValue v = new TaskAttributeValue();
        v.stringValue = value;
        return v;
    }
    
    public static TaskAttributeValue ofNumeric(BigDecimal value) {
        TaskAttributeValue v = new TaskAttributeValue();
        v.numericValue = value;
        return v;
    }
    
    public static TaskAttributeValue ofDate(OffsetDateTime value) {
        TaskAttributeValue v = new TaskAttributeValue();
        v.dateValue = value;
        return v;
    }
    
    public static TaskAttributeValue ofBoolean(Boolean value) {
        TaskAttributeValue v = new TaskAttributeValue();
        v.booleanValue = value;
        return v;
    }
    
    public static TaskAttributeValue ofEntity(String entityRef) {
        TaskAttributeValue v = new TaskAttributeValue();
        v.entityRef = entityRef;
        return v;
    }
    
    public static TaskAttributeValue ofText(String text) {
        TaskAttributeValue v = new TaskAttributeValue();
        v.textValue = text;
        return v;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public TaskAttribute getAttribute() {
        return attribute;
    }
    
    public void setAttribute(TaskAttribute attribute) {
        this.attribute = attribute;
    }
    
    public String getStringValue() {
        return stringValue;
    }
    
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }
    
    public BigDecimal getNumericValue() {
        return numericValue;
    }
    
    public void setNumericValue(BigDecimal numericValue) {
        this.numericValue = numericValue;
    }
    
    public OffsetDateTime getDateValue() {
        return dateValue;
    }
    
    public void setDateValue(OffsetDateTime dateValue) {
        this.dateValue = dateValue;
    }
    
    public Boolean getBooleanValue() {
        return booleanValue;
    }
    
    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }
    
    public String getEntityRef() {
        return entityRef;
    }
    
    public void setEntityRef(String entityRef) {
        this.entityRef = entityRef;
    }
    
    public String getTextValue() {
        return textValue;
    }
    
    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }
}
