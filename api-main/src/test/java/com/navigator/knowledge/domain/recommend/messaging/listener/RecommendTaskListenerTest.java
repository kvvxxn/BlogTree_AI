package com.navigator.knowledge.domain.recommend.messaging.listener;

import com.navigator.knowledge.domain.recommend.entity.Recommendation;
import com.navigator.knowledge.domain.recommend.messaging.dto.RecommendTaskResponseMessage;
import com.navigator.knowledge.domain.recommend.service.RecommendationService;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.entity.TaskType;
import com.navigator.knowledge.domain.task.service.TaskFailureHandler;
import com.navigator.knowledge.domain.task.service.TaskService;
import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendTaskListenerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private SseEmitterService sseEmitterService;

    @Mock
    private TaskFailureHandler taskFailureHandler;

    @InjectMocks
    private RecommendTaskListener recommendTaskListener;

    @Test
    @DisplayName("SUCCESS 응답은 추천 저장 이후에만 task 상태를 SUCCESS로 변경한다")
    void receiveRecommendResponse_successUpdatesTaskStatusLast() {
        String taskId = "task-1";
        Long userId = 10L;
        Task task = Task.builder()
            .taskId(taskId)
            .userId(userId)
            .taskType(TaskType.RECOMMEND)
            .status(TaskStatus.PROCESSING)
            .expiresAt(LocalDateTime.now().plusSeconds(45))
            .build();
        Recommendation recommendation = Recommendation.builder()
            .task(task)
            .userId(userId)
            .reason("reason")
            .category("Backend")
            .topic("Database")
            .keyword("PostgreSQL")
            .build();
        RecommendTaskResponseMessage response = new RecommendTaskResponseMessage(
            taskId,
            userId,
            "SUCCESS",
            new RecommendTaskResponseMessage.ResultData("reason", "Backend", "Database", "PostgreSQL"),
            null
        );

        when(taskService.getTask(taskId)).thenReturn(task);
        when(recommendationService.findOrCreateRecommendation(task, userId, "reason", "Backend", "Database", "PostgreSQL"))
            .thenReturn(recommendation);

        recommendTaskListener.receiveRecommendResponse(response);

        InOrder inOrder = inOrder(taskService, recommendationService, sseEmitterService);
        inOrder.verify(taskService).getTask(taskId);
        inOrder.verify(recommendationService).findOrCreateRecommendation(task, userId, "reason", "Backend", "Database", "PostgreSQL");
        inOrder.verify(taskService).updateTaskStatus(taskId, TaskStatus.SUCCESS);
        inOrder.verify(sseEmitterService).sendEvent(eq(taskId), eq("success"), anyMap());
        inOrder.verify(sseEmitterService).complete(taskId);
    }

    @Test
    @DisplayName("알 수 없는 status는 무시하지 않고 FAILED 처리한다")
    void receiveRecommendResponse_unknownStatusMarksTaskFailed() {
        RecommendTaskResponseMessage response = new RecommendTaskResponseMessage(
            "task-2",
            12L,
            "UNKNOWN",
            null,
            null
        );

        recommendTaskListener.receiveRecommendResponse(response);

        verify(taskFailureHandler).handle(
            eq("task-2"),
            eq("UNKNOWN"),
            org.mockito.ArgumentMatchers.argThat(exception ->
                exception instanceof BusinessException
                    && ((BusinessException) exception).getErrorCode() == ErrorCode.BAD_REQUEST
                    && "Unknown status in recommend response: UNKNOWN".equals(exception.getMessage()))
        );
    }

    @Test
    @DisplayName("예상치 못한 예외는 상세 내용을 노출하지 않고 내부 오류로 처리한다")
    void receiveRecommendResponse_unexpectedExceptionHandledAsInternalError() {
        String taskId = "task-3";
        Long userId = 13L;
        Task task = Task.builder()
            .taskId(taskId)
            .userId(userId)
            .taskType(TaskType.RECOMMEND)
            .status(TaskStatus.PROCESSING)
            .expiresAt(LocalDateTime.now().plusSeconds(45))
            .build();
        RecommendTaskResponseMessage response = new RecommendTaskResponseMessage(
            taskId,
            userId,
            "SUCCESS",
            new RecommendTaskResponseMessage.ResultData("reason", "Backend", "Infra", "Redis"),
            null
        );

        when(taskService.getTask(taskId)).thenReturn(task);
        when(recommendationService.findOrCreateRecommendation(task, userId, "reason", "Backend", "Infra", "Redis"))
            .thenThrow(new RuntimeException("db connection refused"));

        recommendTaskListener.receiveRecommendResponse(response);

        verify(taskFailureHandler).handleUnexpected(
            eq(taskId),
            eq("SUCCESS"),
            isA(RuntimeException.class)
        );
    }
}
