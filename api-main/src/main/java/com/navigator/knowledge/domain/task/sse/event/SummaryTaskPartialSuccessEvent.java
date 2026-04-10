package com.navigator.knowledge.domain.task.sse.event;

public record SummaryTaskPartialSuccessEvent(
    String taskId,
    Long summaryId,
    String category,
    String topic,
    String keyword,
    String summaryContent
) implements TaskSseEvent {

    @Override
    public TaskSseEventName eventName() {
        return TaskSseEventName.PARTIAL_SUCCESS;
    }

    @Override
    public Object payload() {
        return this;
    }
}
