package com.example.kafka.producer.service;

import com.example.kafka.producer.entity.OutboxMessage;
import com.example.kafka.producer.repository.OutboxMessageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service that polls the outbox table and publishes unpublished messages to Kafka.
 * Implements the transactional outbox pattern.
 */
@Service
public class OutboxPollingService {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxPollingService.class);
    
    private final OutboxMessageRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter publishedCounter;
    private final Counter failedCounter;
    
    @Value("${app.outbox.batch-size}")
    private int batchSize;
    
    public OutboxPollingService(OutboxMessageRepository outboxRepository,
                                KafkaTemplate<String, String> kafkaTemplate,
                                MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.publishedCounter = Counter.builder("outbox.messages.published")
            .description("Number of messages published from outbox")
            .register(meterRegistry);
        this.failedCounter = Counter.builder("outbox.messages.failed")
            .description("Number of messages that failed to publish")
            .register(meterRegistry);
    }
    
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms}")
    public void pollAndPublish() {
        List<OutboxMessage> unpublishedMessages = outboxRepository.findUnpublishedMessages(
            PageRequest.of(0, batchSize)
        );
        
        if (unpublishedMessages.isEmpty()) {
            return;
        }
        
        logger.info("Found {} unpublished messages in outbox", unpublishedMessages.size());
        
        for (OutboxMessage message : unpublishedMessages) {
            try {
                publishMessage(message);
            } catch (Exception e) {
                logger.error("Failed to publish message {}: {}", message.getId(), e.getMessage(), e);
                failedCounter.increment();
            }
        }
    }
    
    private void publishMessage(OutboxMessage outboxMessage) {
        logger.debug("Publishing message {} to topic {} with key {}", 
            outboxMessage.getId(), outboxMessage.getTopic(), outboxMessage.getMessageKey());
        
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                outboxMessage.getTopic(),
                outboxMessage.getMessageKey(),
                outboxMessage.getPayload()
            );
            
            // Wait for the send to complete to ensure proper ordering in tests
            SendResult<String, String> result = future.get();
            
            // Mark as published in a transaction
            markAsPublished(outboxMessage);
            publishedCounter.increment();
            logger.info("Successfully published message {} (client: {}) to partition {}", 
                outboxMessage.getId(), 
                outboxMessage.getClientId(),
                result.getRecordMetadata().partition());
            
        } catch (Exception e) {
            logger.error("Failed to publish message {}: {}", outboxMessage.getId(), e.getMessage());
            failedCounter.increment();
            throw new RuntimeException("Failed to publish message", e);
        }
    }
    
    @Transactional
    public void markAsPublished(OutboxMessage outboxMessage) {
        // Reload the entity to ensure it's in the current persistence context
        OutboxMessage managed = outboxRepository.findById(outboxMessage.getId())
            .orElseThrow(() -> new RuntimeException("OutboxMessage not found: " + outboxMessage.getId()));
        managed.markPublished();
        outboxRepository.save(managed);
    }
    
    public long getUnpublishedCount() {
        return outboxRepository.countByPublished(false);
    }
    
    public long getPublishedCount() {
        return outboxRepository.countByPublished(true);
    }
}
