package com.navigator.knowledge.domain.task.sse.event;

public record SummaryTaskSuccessEvent(
    String taskId,
    Long summaryId,
    String category,
    String topic,
    String keyword,
    String summaryContent
) implements TaskSseEvent {

    @Override
    public TaskSseEventName eventName() {
        return TaskSseEventName.SUCCESS;
    }

    @Override
    public Object payload() {
        return this;
    }
}
