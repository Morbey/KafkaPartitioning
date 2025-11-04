package com.example.kafka.consumer.service;

import com.example.kafka.consumer.entity.*;
import com.example.kafka.consumer.repository.MessageRecordRepository;
import com.example.kafka.consumer.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class TaskConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskConsumerService.class);
    private static final Random random = new Random();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final MessageRecordRepository messageRecordRepository;
    private final TaskRepository taskRepository;
    
    @Value("${app.processing.min-delay-seconds}")
    private int minDelaySeconds;
    
    @Value("${app.processing.max-delay-seconds}")
    private int maxDelaySeconds;
    
    public TaskConsumerService(MessageRecordRepository messageRecordRepository, 
                               TaskRepository taskRepository) {
        this.messageRecordRepository = messageRecordRepository;
        this.taskRepository = taskRepository;
    }
    
    @KafkaListener(topics = "${app.kafka.topic}", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void consumeMessage(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            Acknowledgment acknowledgment) {
        
        long startTime = System.currentTimeMillis();
        OffsetDateTime receivedAt = OffsetDateTime.now();
        
        logger.info("========================================");
        logger.info("Received message on topic: {}, partition: {}, offset: {}", topic, partition, offset);
        logger.info("Key: {}", key);
        logger.info("Thread: {}", Thread.currentThread().getName());
        
        try {
            // Create MessageRecord immediately
            MessageRecord messageRecord = new MessageRecord(message, topic, partition, offset, key);
            messageRecord.setReceivedAt(receivedAt);
            messageRecord = messageRecordRepository.save(messageRecord);
            logger.info("Message record created with ID: {}", messageRecord.getId());
            
            // Simulate processing delay (2-20 seconds) - this is the business logic processing time
            int delaySeconds = minDelaySeconds + random.nextInt(maxDelaySeconds - minDelaySeconds + 1);
            logger.info("Simulating processing delay of {} seconds...", delaySeconds);
            Thread.sleep(delaySeconds * 1000L);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Parse and persist task hierarchy
            try {
                parseAndPersistTask(message);
                logger.info("Task hierarchy persisted successfully");
            } catch (Exception e) {
                logger.warn("Could not parse message as Task structure: {}", e.getMessage());
            }
            
            // Mark message as processed (this happens after processing delay, before DB write)
            messageRecord.markProcessed(processingTime);
            messageRecordRepository.save(messageRecord);
            
            logger.info("Message processed in {} ms (including {} s simulated delay)", processingTime, delaySeconds);
            logger.info("========================================");
            
            // Manual acknowledgment - commit offset only after successful processing
            acknowledgment.acknowledge();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Message processing interrupted", e);
            throw new RuntimeException("Message processing interrupted", e);
        } catch (Exception e) {
            logger.error("Error processing message", e);
            throw new RuntimeException("Error processing message", e);
        }
    }
    
    private void parseAndPersistTask(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            
            String taskId = rootNode.path("taskId").asText("unknown");
            Task task = new Task(taskId, message);
            
            // Parse attributes if present
            JsonNode attributesNode = rootNode.path("attributes");
            if (attributesNode.isArray()) {
                for (JsonNode attrNode : attributesNode) {
                    String attrName = attrNode.path("name").asText();
                    String attrTypeStr = attrNode.path("type").asText("STRING");
                    AttributeType attrType = parseAttributeType(attrTypeStr);
                    
                    TaskAttribute attribute = new TaskAttribute(attrName, attrType);
                    
                    // Parse values
                    JsonNode valuesNode = attrNode.path("values");
                    if (valuesNode.isArray()) {
                        for (JsonNode valueNode : valuesNode) {
                            TaskAttributeValue value = parseAttributeValue(valueNode, attrType);
                            if (value != null) {
                                attribute.addValue(value);
                            }
                        }
                    }
                    
                    task.addAttribute(attribute);
                }
            }
            
            taskRepository.save(task);
            
        } catch (Exception e) {
            logger.debug("Message is not in expected Task format, skipping task parsing: {}", e.getMessage());
        }
    }
    
    private AttributeType parseAttributeType(String typeStr) {
        try {
            return AttributeType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AttributeType.STRING;
        }
    }
    
    private TaskAttributeValue parseAttributeValue(JsonNode valueNode, AttributeType type) {
        try {
            switch (type) {
                case NUMERIC:
                    if (valueNode.isNumber()) {
                        return TaskAttributeValue.ofNumeric(new BigDecimal(valueNode.asText()));
                    }
                    break;
                case DATE:
                    if (valueNode.isTextual()) {
                        return TaskAttributeValue.ofDate(
                            OffsetDateTime.parse(valueNode.asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        );
                    }
                    break;
                case BOOLEAN:
                    if (valueNode.isBoolean()) {
                        return TaskAttributeValue.ofBoolean(valueNode.asBoolean());
                    }
                    break;
                case ENTITY:
                    if (valueNode.isTextual()) {
                        return TaskAttributeValue.ofEntity(valueNode.asText());
                    }
                    break;
                case TEXT:
                    return TaskAttributeValue.ofText(valueNode.asText());
                default:
                case STRING:
                    return TaskAttributeValue.ofString(valueNode.asText());
            }
        } catch (Exception e) {
            logger.warn("Could not parse attribute value: {}", e.getMessage());
        }
        return null;
    }
}
