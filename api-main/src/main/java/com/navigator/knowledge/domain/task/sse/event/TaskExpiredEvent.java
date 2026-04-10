package com.navigator.knowledge.domain.task.sse.event;

public record TaskExpiredEvent(
    String taskId,
    String code,
    String message
) implements TaskSseEvent {

    @Override
    public TaskSseEventName eventName() {
        return TaskSseEventName.EXPIRED;
    }

    @Override
    public Object payload() {
        return this;
    }
}
