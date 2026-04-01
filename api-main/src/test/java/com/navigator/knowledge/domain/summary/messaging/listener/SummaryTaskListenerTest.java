package com.navigator.knowledge.domain.summary.messaging.listener;

import com.navigator.knowledge.domain.summary.entity.Summary;
import com.navigator.knowledge.domain.summary.messaging.dto.SummaryTaskResponseMessage;
import com.navigator.knowledge.domain.summary.service.SummaryService;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.service.SseEmitterService;
import com.navigator.knowledge.domain.task.service.TaskService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import com.navigator.knowledge.global.infra.ai.TextEmbeddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryTaskListenerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private SummaryService summaryService;

    @Mock
    private TextEmbeddingService textEmbeddingService;

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private SseEmitterService sseEmitterService;

    @InjectMocks
    private SummaryTaskListener summaryTaskListener;

    @Test
    @DisplayName("SUCCESS 응답은 저장과 그래프 반영 이후에만 task 상태를 SUCCESS로 변경한다")
    void receiveSummaryResponse_successUpdatesTaskStatusLast() {
        String taskId = "task-1";
        Long userId = 10L;
        Task task = Task.builder()
            .taskId(taskId)
            .userId(userId)
            .sourceUrl("https://example.com")
            .status(TaskStatus.PROCESSING)
            .build();
        Summary summary = Summary.builder()
            .task(task)
            .userId(userId)
            .sourceUrl(task.getSourceUrl())
            .content("summary")
            .build();
        SummaryTaskResponseMessage response = new SummaryTaskResponseMessage(
            taskId,
            userId,
            "SUCCESS",
            new SummaryTaskResponseMessage.ResultData(
                "summary",
                new SummaryTaskResponseMessage.KnowledgeTree("Backend", "Database", "PostgreSQL")
            ),
            null
        );

        when(taskService.getTask(taskId)).thenReturn(task);
        when(summaryService.findOrCreateSummary(task, userId, task.getSourceUrl(), "summary")).thenReturn(summary);
        when(textEmbeddingService.embedText("summary")).thenReturn(List.of(0.1, 0.2, 0.3));

        summaryTaskListener.receiveSummaryResponse(response);

        InOrder inOrder = inOrder(taskService, summaryService, textEmbeddingService, knowledgeService, sseEmitterService);
        inOrder.verify(taskService).getTask(taskId);
        inOrder.verify(summaryService).findOrCreateSummary(task, userId, task.getSourceUrl(), "summary");
        inOrder.verify(textEmbeddingService).embedText("summary");
        inOrder.verify(knowledgeService).saveKnowledgePath(userId, "Backend", "Database", "PostgreSQL", null, List.of(0.1, 0.2, 0.3));
        inOrder.verify(taskService).updateTaskStatus(taskId, TaskStatus.SUCCESS);
        inOrder.verify(sseEmitterService).sendEvent(eq(taskId), eq("success"), anyMap());
        inOrder.verify(sseEmitterService).complete(taskId);
    }

    @Test
    @DisplayName("PARTIAL_SUCCESS에서 기존 키워드 연결에 실패하면 task를 FAILED로 전환하고 실패 SSE를 보낸다")
    void receiveSummaryResponse_partialSuccessFailureMarksTaskFailed() {
        String taskId = "task-2";
        Long userId = 11L;
        Task task = Task.builder()
            .taskId(taskId)
            .userId(userId)
            .sourceUrl("https://example.com/2")
            .status(TaskStatus.PROCESSING)
            .build();
        Summary summary = Summary.builder()
            .task(task)
            .userId(userId)
            .sourceUrl(task.getSourceUrl())
            .content("summary")
            .build();
        SummaryTaskResponseMessage response = new SummaryTaskResponseMessage(
            taskId,
            userId,
            "PARTIAL_SUCCESS",
            new SummaryTaskResponseMessage.ResultData(
                "summary",
                new SummaryTaskResponseMessage.KnowledgeTree("Backend", "Database", "MySQL")
            ),
            null
        );

        when(taskService.getTask(taskId)).thenReturn(task);
        when(summaryService.findOrCreateSummary(task, userId, task.getSourceUrl(), "summary")).thenReturn(summary);
        when(textEmbeddingService.embedText("summary")).thenReturn(List.of(0.4, 0.5));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("No such keyword"))
            .when(knowledgeService).addSummaryToSimilarKeyword(userId, null, List.of(0.4, 0.5));

        summaryTaskListener.receiveSummaryResponse(response);

        verify(taskService, never()).updateTaskStatus(taskId, TaskStatus.PARTIAL_SUCCESS);
        verify(taskService).updateTaskFailed(taskId, "[LISTENER_PROCESSING_ERROR] No such keyword");
        verify(sseEmitterService).sendEvent(eq(taskId), eq("failed"), anyMap());
        verify(sseEmitterService).complete(taskId);
    }

    @Test
    @DisplayName("알 수 없는 status는 무시하지 않고 FAILED 처리한다")
    void receiveSummaryResponse_unknownStatusMarksTaskFailed() {
        SummaryTaskResponseMessage response = new SummaryTaskResponseMessage(
            "task-3",
            12L,
            "UNKNOWN",
            null,
            null
        );

        summaryTaskListener.receiveSummaryResponse(response);

        verify(taskService).updateTaskFailed("task-3", "[LISTENER_PROCESSING_ERROR] Unknown status in summary response: UNKNOWN");
        verify(sseEmitterService).sendEvent(eq("task-3"), eq("failed"), anyMap());
        verify(sseEmitterService).complete("task-3");
    }
}
