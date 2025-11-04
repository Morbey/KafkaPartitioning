package com.example.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MessageProducerController {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerController.class);
    
    private final StreamBridge streamBridge;

    public MessageProducerController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishMessage(@RequestBody MessageRequest request) {
        try {
            String message = request.getMessage();
            String partitionKey = request.getPartitionKey();
            
            logger.info("Publishing message: {} with partition key: {}", message, partitionKey);
            
            // Send message with partition key
            if (partitionKey != null && !partitionKey.isEmpty()) {
                streamBridge.send("produceMessage-out-0", partitionKey, message);
            } else {
                streamBridge.send("produceMessage-out-0", message);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", message);
            response.put("partitionKey", partitionKey);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error publishing message", e);
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
            
            logger.info("Publishing {} messages with prefix: {}", count, prefix);
            
            for (int i = 0; i < count; i++) {
                String message = prefix + "-" + i;
                String partitionKey = "key-" + (i % 3); // Distribute across 3 partitions
                streamBridge.send("produceMessage-out-0", partitionKey, message);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("published", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error publishing batch", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "producer-app");
        return ResponseEntity.ok(health);
    }
}
