package com.navigator.knowledge.domain.task.sse;

import com.navigator.knowledge.domain.task.sse.event.TaskSseEvent;

public record CachedSseEvent(TaskSseEvent event) {
}
