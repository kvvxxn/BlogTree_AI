package com.navigator.knowledge.domain.summary.repository;

import com.navigator.knowledge.domain.stats.dto.CategoryReadCountDto;
import com.navigator.knowledge.domain.stats.dto.RecentKeywordDto;
import com.navigator.knowledge.domain.stats.dto.TopicReadCountDto;
import com.navigator.knowledge.domain.summary.entity.Summary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {

    @EntityGraph(attributePaths = {"task", "keyword", "keyword.topic", "keyword.topic.category"})
    @Query("select s from Summary s where s.summaryId = :summaryId")
    Optional<Summary> findDetailedBySummaryId(@Param("summaryId") Long summaryId);

    Optional<Summary> findByTask_TaskId(String taskId);

    Optional<Summary> findByUserId(Long userId);

    long countByUserId(Long userId);

    @Query("""
            select new com.navigator.knowledge.domain.stats.dto.CategoryReadCountDto(c.name, count(s))
            from Summary s
            join s.keyword k
            join k.topic t
            join t.category c
            where s.userId = :userId
              and c.userId = :userId
            group by c.name
            order by count(s) desc, c.name asc
            """)
    List<CategoryReadCountDto> findCategoryReadCounts(@Param("userId") Long userId);

    @Query("""
            select new com.navigator.knowledge.domain.stats.dto.TopicReadCountDto(t.name, count(s))
            from Summary s
            join s.keyword k
            join k.topic t
            join t.category c
            where s.userId = :userId
              and c.userId = :userId
            group by t.name
            order by count(s) desc, t.name asc
            """)
    List<TopicReadCountDto> findTopTopicReadCounts(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select new com.navigator.knowledge.domain.stats.dto.RecentKeywordDto(k.name, s.createdAt)
            from Summary s
            join s.keyword k
            join k.topic t
            join t.category c
            where s.userId = :userId
              and c.userId = :userId
            order by s.createdAt desc, s.summaryId desc
            """)
    List<RecentKeywordDto> findRecentKeywords(@Param("userId") Long userId, Pageable pageable);

}
