package com.example.kafka.producer.repository;

import com.example.kafka.producer.entity.OutboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    
    @Query("SELECT o FROM OutboxMessage o WHERE o.published = false ORDER BY o.createdAt ASC")
    List<OutboxMessage> findUnpublishedMessages(Pageable pageable);
    
    @Modifying
    @Query("UPDATE OutboxMessage o SET o.published = true, o.publishedAt = :publishedAt WHERE o.id = :id")
    void markAsPublished(Long id, OffsetDateTime publishedAt);
    
    long countByPublished(boolean published);
}
