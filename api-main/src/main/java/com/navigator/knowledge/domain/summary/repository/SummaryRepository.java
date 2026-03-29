package com.navigator.knowledge.domain.summary.repository;

import com.navigator.knowledge.domain.summary.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {

    Optional<Summary> findByTask_TaskId(String taskId);

    Optional<Summary> findByUserId(Long userId);

}