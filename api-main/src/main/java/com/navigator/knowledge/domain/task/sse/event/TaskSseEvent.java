package com.navigator.knowledge.domain.task.sse.event;

public interface TaskSseEvent {

    String taskId();

    TaskSseEventName eventName();

    Object payload();

    default boolean terminal() {
        return eventName().isTerminal();
    }
}
