package com.example.kafka.producer.service;

import com.example.kafka.producer.entity.OutboxMessage;
import com.example.kafka.producer.repository.OutboxMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that aggregates outbox messages by task_id and publishes task snapshots.
 * This reduces the number of messages sent to Kafka by grouping attribute-level changes
 * into a single snapshot per task.
 */
@Service
public class OutboxAggregatorService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxAggregatorService.class);

    private final OutboxMessageRepository outboxRepo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.outbox.aggregator-interval-ms:500}")
    private long aggregatorIntervalMs;

    @Value("${app.outbox.debounce-ms:200}")
    private long debounceMs;

    @Value("${app.kafka.snapshot-topic:task-snapshots}")
    private String snapshotTopic;

    public OutboxAggregatorService(OutboxMessageRepository outboxRepo, 
                                  KafkaTemplate<String, String> kafka) {
        this.outboxRepo = outboxRepo;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${app.outbox.aggregator-interval-ms:500}")
    @Transactional
    public void aggregateAndPublish() {
        // Calculate threshold - only process messages older than debounce window
        OffsetDateTime threshold = OffsetDateTime.now().minusNanos(debounceMs * 1_000_000);
        
        // Load unpublished rows older than debounce window
        var rows = outboxRepo.findUnpublishedOlderThan(threshold);
        
        if (rows.isEmpty()) {
            return;
        }
        
        logger.debug("Found {} unpublished messages for aggregation", rows.size());
        
        // Group by taskId
        var byTask = rows.stream()
            .collect(Collectors.groupingBy(OutboxMessage::getTaskId));
        
        for (var entry : byTask.entrySet()) {
            String taskId = entry.getKey();
            var group = entry.getValue();
            
            try {
                // Build snapshot from attribute payloads (merge logic depends on domain)
                Map<String, Object> snapshot = new HashMap<>();
                snapshot.put("taskId", taskId);
                snapshot.put("attributes", mergeAttributes(group));
                snapshot.put("timestamp", OffsetDateTime.now().toString());
                
                String payload = mapper.writeValueAsString(snapshot);
                
                // Publish snapshot keyed by taskId to ensure ordering per task
                kafka.send(snapshotTopic, taskId, payload);
                
                // Mark original rows as published
                List<Long> ids = group.stream()
                    .map(OutboxMessage::getId)
                    .collect(Collectors.toList());
                outboxRepo.markAsPublished(ids, OffsetDateTime.now());
                
                logger.info("Aggregated and published snapshot for task {} ({} messages)", 
                    taskId, group.size());
                
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize snapshot for task {}: {}", taskId, e.getMessage());
                // Mark group as published to avoid infinite loop
                List<Long> ids = group.stream()
                    .map(OutboxMessage::getId)
                    .collect(Collectors.toList());
                outboxRepo.markAsPublished(ids, OffsetDateTime.now());
            } catch (Exception e) {
                logger.error("Failed to publish snapshot for task {}: {}", taskId, e.getMessage(), e);
                // Don't mark as published - retry on next iteration
            }
        }
    }

    /**
     * Merge attribute-level payloads into a single list of attributes.
     * Uses LinkedHashMap to preserve order while ensuring latest value per attribute name.
     */
    private List<Map<String, Object>> mergeAttributes(List<OutboxMessage> msgs) {
        Map<String, Map<String, Object>> attrs = new LinkedHashMap<>();
        
        for (OutboxMessage m : msgs) {
            Map<String, Object> p = parsePayload(m.getPayload());
            String name = (String) p.get("attributeName");
            if (name != null) {
                attrs.put(name, p);
            } else {
                // If no attributeName, it might be a full task payload
                // In that case, extract all attributes if present
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> attributes = (List<Map<String, Object>>) p.get("attributes");
                if (attributes != null) {
                    for (Map<String, Object> attr : attributes) {
                        String attrName = (String) attr.get("name");
                        if (attrName != null) {
                            attrs.put(attrName, attr);
                        }
                    }
                }
            }
        }
        
        return new ArrayList<>(attrs.values());
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(payload, Map.class);
            return map;
        } catch (Exception e) {
            logger.warn("Failed to parse payload: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
