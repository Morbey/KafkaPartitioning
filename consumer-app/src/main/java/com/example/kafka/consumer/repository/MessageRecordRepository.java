package com.example.kafka.consumer.repository;

import com.example.kafka.consumer.entity.MessageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface MessageRecordRepository extends JpaRepository<MessageRecord, Long> {
    
    List<MessageRecord> findByKafkaTopicAndPartition(String topic, Integer partition);
    
    @Query("SELECT m FROM MessageRecord m WHERE m.receivedAt >= :since ORDER BY m.receivedAt DESC")
    List<MessageRecord> findRecentMessages(OffsetDateTime since);
    
    @Query("SELECT AVG(m.processingDurationMs) FROM MessageRecord m WHERE m.processingDurationMs IS NOT NULL")
    Double getAverageProcessingDuration();
}
