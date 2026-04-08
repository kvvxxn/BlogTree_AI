package com.navigator.knowledge.domain.recommend.repository;

import com.navigator.knowledge.domain.recommend.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findByTask_TaskId(String taskId);
}
