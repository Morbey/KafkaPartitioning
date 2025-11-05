package com.example.kafka.producer.service;

import com.example.kafka.producer.entity.OracleOutboxMessage;
import com.example.kafka.producer.repository.OracleOutboxMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Oracle-specific outbox polling service.
 * Polls Oracle outbox table and publishes messages to Kafka.
 * Uses Oracle AQ/JMS internally for transactional guarantees.
 */
@Service
@ConditionalOnProperty(name = "app.outbox.use-oracle", havingValue = "true")
public class OracleOutboxPollingService {

    private static final Logger logger = LoggerFactory.getLogger(OracleOutboxPollingService.class);

    private final OracleOutboxMessageRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.outbox.poll-interval-ms:1000}")
    private long pollIntervalMs;

    @Value("${app.outbox.batch-size:100}")
    private int batchSize;

    public OracleOutboxPollingService(OracleOutboxMessageRepository repository,
                                     KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void pollAndPublish() {
        try {
            // Fetch unpublished messages from Oracle outbox
            List<OracleOutboxMessage> messages = repository.findUnpublishedMessages(
                PageRequest.of(0, batchSize)
            );

            if (messages.isEmpty()) {
                return;
            }

            logger.debug("Processing {} unpublished messages from Oracle outbox", messages.size());

            for (OracleOutboxMessage message : messages) {
                try {
                    // Publish to Kafka
                    kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                logger.debug("Successfully published message {} to topic {} with key {}",
                                    message.getId(), message.getTopic(), message.getMessageKey());
                            } else {
                                logger.error("Failed to publish message {} to Kafka: {}",
                                    message.getId(), ex.getMessage());
                            }
                        });

                    // Mark as published
                    repository.markAsPublished(message.getId(), OffsetDateTime.now());
                    
                    logger.info("Published Oracle outbox message {} to topic {} with key {}",
                        message.getId(), message.getTopic(), message.getMessageKey());

                } catch (Exception e) {
                    logger.error("Error publishing Oracle outbox message {}: {}",
                        message.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Error in Oracle outbox polling: {}", e.getMessage(), e);
        }
    }

    public long getUnpublishedCount() {
        return repository.countByPublished(false);
    }

    public long getPublishedCount() {
        return repository.countByPublished(true);
    }
}
