package com.example.kafka.consumer.repository;

import com.example.kafka.consumer.entity.TaskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskSnapshotRepository extends JpaRepository<TaskSnapshot, Long> {
    
    Optional<TaskSnapshot> findByTaskId(String taskId);
    
    boolean existsByTaskId(String taskId);
}
