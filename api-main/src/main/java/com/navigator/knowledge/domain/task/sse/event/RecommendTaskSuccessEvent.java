package com.navigator.knowledge.domain.task.sse.event;

public record RecommendTaskSuccessEvent(
    String taskId,
    Long recommendationId,
    String reason,
    String category,
    String topic,
    String keyword
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
