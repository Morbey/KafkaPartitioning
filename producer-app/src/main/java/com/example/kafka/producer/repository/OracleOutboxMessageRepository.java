package com.example.kafka.producer.repository;

import com.example.kafka.producer.entity.OracleOutboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface OracleOutboxMessageRepository extends JpaRepository<OracleOutboxMessage, Long> {
    
    @Query("SELECT o FROM OracleOutboxMessage o WHERE o.published = false ORDER BY o.createdAt ASC")
    List<OracleOutboxMessage> findUnpublishedMessages(Pageable pageable);
    
    @Query("SELECT o FROM OracleOutboxMessage o WHERE o.published = false AND o.taskId IS NOT NULL " +
           "AND o.createdAt < :threshold ORDER BY o.createdAt ASC")
    List<OracleOutboxMessage> findUnpublishedOlderThan(@Param("threshold") OffsetDateTime threshold);
    
    @Modifying
    @Query("UPDATE OracleOutboxMessage o SET o.published = true, o.publishedAt = :publishedAt WHERE o.id = :id")
    void markAsPublished(Long id, OffsetDateTime publishedAt);
    
    @Modifying
    @Query("UPDATE OracleOutboxMessage o SET o.published = true, o.publishedAt = :publishedAt WHERE o.id IN :ids")
    void markAsPublished(@Param("ids") List<Long> ids, @Param("publishedAt") OffsetDateTime publishedAt);
    
    long countByPublished(boolean published);
}
