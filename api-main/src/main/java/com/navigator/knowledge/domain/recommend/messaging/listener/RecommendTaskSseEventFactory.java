package com.navigator.knowledge.domain.recommend.messaging.listener;

import com.navigator.knowledge.domain.recommend.entity.Recommendation;
import com.navigator.knowledge.domain.task.sse.event.RecommendTaskSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class RecommendTaskSseEventFactory {

    public RecommendTaskSuccessEvent success(String taskId, Recommendation recommendation) {
        return new RecommendTaskSuccessEvent(
            taskId,
            recommendation.getRecommendationId(),
            recommendation.getReason(),
            recommendation.getCategory(),
            recommendation.getTopic(),
            recommendation.getKeyword()
        );
    }
}
