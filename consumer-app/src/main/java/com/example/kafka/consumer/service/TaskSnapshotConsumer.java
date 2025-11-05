package com.example.kafka.consumer.service;

import com.example.kafka.consumer.entity.TaskSnapshot;
import com.example.kafka.consumer.repository.TaskSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Consumer service for task snapshot messages.
 * Updates the materialized read-model (task_snapshots table) with complete task state.
 */
@Service
public class TaskSnapshotConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TaskSnapshotConsumer.class);
    
    private final TaskSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaskSnapshotConsumer(TaskSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @KafkaListener(
        topics = "${app.kafka.snapshot-topic:task-snapshots}",
        groupId = "${app.kafka.snapshot-consumer-group:task-snapshot-consumer-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeSnapshot(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String taskId = record.key();
        String payload = record.value();
        
        logger.debug("Received snapshot for task {} from partition {} offset {}", 
            taskId, record.partition(), record.offset());
        
        try {
            // Validate payload
            @SuppressWarnings("unchecked")
            Map<String, Object> snapshotData = objectMapper.readValue(payload, Map.class);
            String extractedTaskId = (String) snapshotData.get("taskId");
            
            if (extractedTaskId == null || !extractedTaskId.equals(taskId)) {
                logger.warn("TaskId mismatch: key={}, payload.taskId={}", taskId, extractedTaskId);
            }
            
            // Find existing snapshot or create new one
            Optional<TaskSnapshot> existing = snapshotRepository.findByTaskId(taskId);
            
            TaskSnapshot snapshot;
            if (existing.isPresent()) {
                snapshot = existing.get();
                snapshot.updateSnapshot(payload);
                logger.debug("Updated snapshot for task {} (version {})", taskId, snapshot.getVersion());
            } else {
                snapshot = new TaskSnapshot(taskId, payload);
                logger.debug("Created new snapshot for task {}", taskId);
            }
            
            // Store Kafka metadata
            snapshot.setKafkaOffset(record.offset());
            snapshot.setKafkaPartition(record.partition());
            
            // Save to database
            snapshotRepository.save(snapshot);
            
            // Commit offset only after successful persistence
            acknowledgment.acknowledge();
            
            logger.info("Successfully processed snapshot for task {} (partition: {}, offset: {})", 
                taskId, record.partition(), record.offset());
            
        } catch (Exception e) {
            logger.error("Failed to process snapshot for task {}: {}", taskId, e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed
            throw new RuntimeException("Failed to process snapshot for task " + taskId, e);
        }
    }
}
