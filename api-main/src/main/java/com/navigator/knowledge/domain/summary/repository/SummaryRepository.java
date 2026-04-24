package com.navigator.knowledge.domain.summary.repository;

import com.navigator.knowledge.domain.summary.entity.Summary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {

    @EntityGraph(attributePaths = {"task", "keyword", "keyword.topic", "keyword.topic.category"})
    @Query("select s from Summary s where s.summaryId = :summaryId")
    Optional<Summary> findDetailedBySummaryId(@Param("summaryId") Long summaryId);

    Optional<Summary> findByTask_TaskId(String taskId);

    Optional<Summary> findByUserId(Long userId);

}
