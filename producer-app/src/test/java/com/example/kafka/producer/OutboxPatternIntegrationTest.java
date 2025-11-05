package com.example.kafka.producer;

import com.example.kafka.producer.entity.OutboxMessage;
import com.example.kafka.producer.repository.OutboxMessageRepository;
import com.example.kafka.producer.service.OutboxPollingService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 3, topics = {"task-topic"})
@DirtiesContext
public class OutboxPatternIntegrationTest {
    
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
    private OutboxMessageRepository outboxRepository;
    
    @Autowired
    private OutboxPollingService outboxPollingService;
    
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    
    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }
    
    @Test
    void testOutboxMessageIsPublishedToKafka() throws Exception {
        // Given - create an outbox message
        OutboxMessage outboxMessage = new OutboxMessage(
            "Test message content",
            "client-1",
            "task-topic",
            "client-1"
        );
        outboxRepository.save(outboxMessage);
        
        // Create a Kafka consumer to verify publication
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            embeddedKafkaBroker.getBrokersAsString(),
            "test-group",
            "true"
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
            consumerProps,
            new StringDeserializer(),
            new StringDeserializer()
        ).createConsumer();
        
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "task-topic");
        
        // When - trigger polling (wait for scheduled task)
        await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                OutboxMessage updated = outboxRepository.findById(outboxMessage.getId()).orElseThrow();
                assertThat(updated.getPublished()).isTrue();
                assertThat(updated.getPublishedAt()).isNotNull();
            });
        
        // Then - verify message was published to Kafka
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(
            consumer,
            Duration.ofSeconds(10)
        );
        
        assertThat(records.count()).isGreaterThan(0);
        
        boolean found = false;
        for (var record : records) {
            if (record.value().equals("Test message content") && record.key().equals("client-1")) {
                found = true;
                assertThat(record.topic()).isEqualTo("task-topic");
                break;
            }
        }
        assertThat(found).isTrue();
        
        consumer.close();
    }
    
    @Test
    void testMultipleMessagesWithDifferentKeysDistributedAcrossPartitions() {
        // Given - create messages for different clients
        for (int i = 0; i < 9; i++) {
            String clientId = "client-" + (i % 3);
            OutboxMessage message = new OutboxMessage(
                "Message " + i,
                clientId,
                "task-topic",
                clientId
            );
            outboxRepository.save(message);
        }
        
        // When - wait for all messages to be published
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                long unpublished = outboxRepository.countByPublished(false);
                assertThat(unpublished).isEqualTo(0);
                
                long published = outboxRepository.countByPublished(true);
                assertThat(published).isEqualTo(9);
            });
        
        // Then - verify all messages are marked as published
        List<OutboxMessage> allMessages = outboxRepository.findAll();
        assertThat(allMessages)
            .hasSize(9)
            .allMatch(OutboxMessage::getPublished)
            .allMatch(m -> m.getPublishedAt() != null);
        
        // Verify we have messages for all 3 clients
        assertThat(allMessages)
            .extracting(OutboxMessage::getClientId)
            .contains("client-0", "client-1", "client-2");
    }
    
    @Test
    void testOutboxStatsEndpoint() {
        // Given - create some published and unpublished messages
        for (int i = 0; i < 5; i++) {
            OutboxMessage message = new OutboxMessage(
                "Message " + i,
                "client-" + i,
                "task-topic",
                "client-" + i
            );
            if (i < 3) {
                message.markPublished();
            }
            outboxRepository.save(message);
        }
        
        // When
        long unpublished = outboxPollingService.getUnpublishedCount();
        long published = outboxPollingService.getPublishedCount();
        
        // Then
        assertThat(unpublished).isEqualTo(2);
        assertThat(published).isEqualTo(3);
    }
}
