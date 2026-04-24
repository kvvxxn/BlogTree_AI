package com.navigator.knowledge.domain.task.sse.event;

public enum TaskSseEventName {
    SUCCESS("SUCCESS", true),
    PARTIAL_SUCCESS("PARTIAL_SUCCESS", true),
    FAILED("FAILED", true),
    EXPIRED("EXPIRED", true);

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
