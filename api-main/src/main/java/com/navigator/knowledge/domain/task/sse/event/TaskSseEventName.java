package com.navigator.knowledge.domain.task.sse.event;

public enum TaskSseEventName {
    SUCCESS("success", true),
    PARTIAL_SUCCESS("partial_success", true),
    FAILED("failed", true),
    EXPIRED("expired", true);

    private final String value;
    private final boolean terminal;

    TaskSseEventName(String value, boolean terminal) {
        this.value = value;
        this.terminal = terminal;
    }

    public String value() {
        return value;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
