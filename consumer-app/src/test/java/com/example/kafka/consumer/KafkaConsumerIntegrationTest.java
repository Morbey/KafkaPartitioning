package com.example.kafka.consumer;

import com.example.kafka.consumer.entity.MessageRecord;
import com.example.kafka.consumer.repository.MessageRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 3, topics = {"task-topic"})
@DirtiesContext
public class KafkaConsumerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private MessageRecordRepository messageRecordRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        messageRecordRepository.deleteAll();
    }
    
    @Test
    void testSingleMessageConsumption() throws Exception {
        // Given
        String message = "Test message";
        String key = "client-1";
        
        // When
        kafkaTemplate.send("task-topic", key, message).get(10, TimeUnit.SECONDS);
        
        // Then - wait for message to be processed (max 30 seconds due to processing delay)
        await()
            .atMost(35, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<MessageRecord> records = messageRecordRepository.findAll();
                assertThat(records).hasSize(1);
                
                MessageRecord record = records.get(0);
                assertThat(record.getRawMessage()).isEqualTo(message);
                assertThat(record.getMessageKey()).isEqualTo(key);
                assertThat(record.getReceivedAt()).isNotNull();
                assertThat(record.getProcessedAt()).isNotNull();
                assertThat(record.getKafkaTopic()).isEqualTo("task-topic");
                assertThat(record.getPartition()).isBetween(0, 2);
                assertThat(record.getProcessingDurationMs()).isGreaterThan(2000L); // At least 2 seconds
            });
    }
    
    @Test
    void testMultipleMessagesWithDifferentKeys() throws Exception {
        // Given - send messages with different keys to distribute across partitions
        Map<String, String> messagesToSend = new HashMap<>();
        messagesToSend.put("client-0", "Message for client 0");
        messagesToSend.put("client-1", "Message for client 1");
        messagesToSend.put("client-2", "Message for client 2");
        
        // When
        for (Map.Entry<String, String> entry : messagesToSend.entrySet()) {
            kafkaTemplate.send("task-topic", entry.getKey(), entry.getValue())
                .get(10, TimeUnit.SECONDS);
        }
        
        // Then - all messages should be processed
        await()
            .atMost(70, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<MessageRecord> records = messageRecordRepository.findAll();
                assertThat(records).hasSize(3);
                
                // Verify all messages were received
                assertThat(records)
                    .extracting(MessageRecord::getRawMessage)
                    .containsExactlyInAnyOrder(
                        "Message for client 0",
                        "Message for client 1", 
                        "Message for client 2"
                    );
                
                // Verify all have been processed
                assertThat(records)
                    .allMatch(r -> r.getProcessedAt() != null)
                    .allMatch(r -> r.getProcessingDurationMs() != null);
            });
    }
    
    @Test
    void testTaskStructuredMessage() throws Exception {
        // Given - a message with task structure
        Map<String, Object> taskMessage = new HashMap<>();
        taskMessage.put("taskId", "TASK-001");
        taskMessage.put("attributes", List.of(
            Map.of(
                "name", "priority",
                "type", "STRING",
                "values", List.of("HIGH")
            ),
            Map.of(
                "name", "dueDate",
                "type", "DATE",
                "values", List.of("2025-12-31T23:59:59Z")
            )
        ));
        
        String jsonMessage = objectMapper.writeValueAsString(taskMessage);
        
        // When
        kafkaTemplate.send("task-topic", "client-1", jsonMessage)
            .get(10, TimeUnit.SECONDS);
        
        // Then
        await()
            .atMost(35, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<MessageRecord> records = messageRecordRepository.findAll();
                assertThat(records).hasSize(1);
                
                MessageRecord record = records.get(0);
                assertThat(record.getRawMessage()).contains("TASK-001");
                assertThat(record.getProcessedAt()).isNotNull();
            });
    }
}
