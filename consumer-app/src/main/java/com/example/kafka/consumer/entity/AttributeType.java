package com.example.kafka.consumer.entity;

/**
 * Enum representing different attribute types
 */
public enum AttributeType {
    STRING,
    NUMERIC,
    DATE,
    BOOLEAN,
    ENTITY,  // Represents a dropdown/reference value
    TEXT     // For long text values
}
