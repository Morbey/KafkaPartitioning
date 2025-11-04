package com.example.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);

    @Bean
    public Consumer<Message<String>> consumeMessage() {
        return message -> {
            String payload = message.getPayload();
            Integer partition = message.getHeaders().get(KafkaHeaders.RECEIVED_PARTITION, Integer.class);
            String topic = message.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC, String.class);
            Long offset = message.getHeaders().get(KafkaHeaders.OFFSET, Long.class);
            
            logger.info("========================================");
            logger.info("Received message: {}", payload);
            logger.info("Topic: {}", topic);
            logger.info("Partition: {}", partition);
            logger.info("Offset: {}", offset);
            logger.info("Thread: {}", Thread.currentThread().getName());
            logger.info("========================================");
        };
    }
}
