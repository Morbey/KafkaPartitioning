package com.example.kafka.consumer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal controller for graceful shutdown.
 * Used by Kubernetes preStop hook to stop consuming before pod termination.
 */
@RestController
@RequestMapping("/internal")
public class InternalController {
    
    private static final Logger logger = LoggerFactory.getLogger(InternalController.class);
    
    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
    
    public InternalController(KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
    }
    
    @PostMapping("/stop-consuming")
    public Map<String, Object> stopConsuming() {
        logger.info("Received request to stop consuming messages (graceful shutdown)");
        
        Collection<MessageListenerContainer> containers = 
            kafkaListenerEndpointRegistry.getAllListenerContainers();
        
        int stoppedCount = 0;
        for (MessageListenerContainer container : containers) {
            if (container.isRunning()) {
                logger.info("Stopping listener container: {}", container.getListenerId());
                container.stop();
                stoppedCount++;
            }
        }
        
        logger.info("Stopped {} listener containers", stoppedCount);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("stoppedContainers", stoppedCount);
        response.put("message", "Consumer listeners stopped gracefully");
        
        return response;
    }
}
