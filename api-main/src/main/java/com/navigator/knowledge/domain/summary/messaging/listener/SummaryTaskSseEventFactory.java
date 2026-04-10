package com.navigator.knowledge.domain.summary.messaging.listener;

import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.task.sse.event.SummaryTaskPartialSuccessEvent;
import com.navigator.knowledge.domain.task.sse.event.SummaryTaskSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class SummaryTaskSseEventFactory {

    public SummaryTaskSuccessEvent success(
        String taskId,
        Summary summary,
        String category,
        String topic,
        String keyword,
        String summaryContent
    ) {
        return new SummaryTaskSuccessEvent(
            taskId,
            summary.getSummaryId(),
            category,
            topic,
            keyword,
            summaryContent
        );
    }

    public SummaryTaskPartialSuccessEvent partialSuccess(
        String taskId,
        Summary summary,
        String category,
        String topic,
        String keyword,
        String summaryContent
    ) {
        return new SummaryTaskPartialSuccessEvent(
            taskId,
            summary.getSummaryId(),
            category,
            topic,
            keyword,
            summaryContent
        );
    }
}
