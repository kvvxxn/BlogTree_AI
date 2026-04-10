package com.navigator.knowledge.domain.task.sse.event;

public record TaskFailedEvent(
    String taskId,
    String code,
    String message
) implements TaskSseEvent {

    @Override
    public TaskSseEventName eventName() {
        return TaskSseEventName.FAILED;
    }

    @Override
    public Object payload() {
        return this;
    }
}
