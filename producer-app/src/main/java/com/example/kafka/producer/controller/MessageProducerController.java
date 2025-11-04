package com.example.kafka.producer.controller;

import com.example.kafka.producer.dto.BatchRequest;
import com.example.kafka.producer.dto.MessageRequest;
import com.example.kafka.producer.entity.OutboxMessage;
import com.example.kafka.producer.repository.OutboxMessageRepository;
import com.example.kafka.producer.service.OutboxPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MessageProducerController {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerController.class);
    
    private final OutboxMessageRepository outboxRepository;
    private final OutboxPollingService outboxPollingService;
    
    @Value("${app.kafka.topic}")
    private String topic;

    public MessageProducerController(OutboxMessageRepository outboxRepository,
                                    OutboxPollingService outboxPollingService) {
        this.outboxRepository = outboxRepository;
        this.outboxPollingService = outboxPollingService;
    }

    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishMessage(@RequestBody MessageRequest request) {
        try {
            String message = request.getMessage();
            String partitionKey = request.getPartitionKey();
            
            if (partitionKey == null || partitionKey.isEmpty()) {
                partitionKey = "default";
            }
            
            logger.info("Adding message to outbox with key: {}", partitionKey);
            
            // Insert into outbox table - will be picked up by the poller
            OutboxMessage outboxMessage = new OutboxMessage(message, partitionKey, topic, partitionKey);
            outboxRepository.save(outboxMessage);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Message added to outbox");
            response.put("outboxId", outboxMessage.getId());
            response.put("partitionKey", partitionKey);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error adding message to outbox", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/publish-batch")
    public ResponseEntity<Map<String, Object>> publishBatch(@RequestBody BatchRequest request) {
        try {
            int count = request.getCount() != null ? request.getCount() : 10;
            String prefix = request.getPrefix() != null ? request.getPrefix() : "Message";
            
            logger.info("Adding {} messages to outbox with prefix: {}", count, prefix);
            
            for (int i = 0; i < count; i++) {
                String message = prefix + "-" + i;
                // Distribute across 3 different clients/keys for partition distribution
                String clientId = "client-" + (i % 3);
                
                OutboxMessage outboxMessage = new OutboxMessage(message, clientId, topic, clientId);
                outboxRepository.save(outboxMessage);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("addedToOutbox", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error adding batch to outbox", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "producer-app");
        health.put("unpublishedMessages", outboxPollingService.getUnpublishedCount());
        health.put("publishedMessages", outboxPollingService.getPublishedCount());
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/outbox/stats")
    public ResponseEntity<Map<String, Object>> outboxStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("unpublished", outboxPollingService.getUnpublishedCount());
        stats.put("published", outboxPollingService.getPublishedCount());
        return ResponseEntity.ok(stats);
    }
}

