package com.navigator.knowledge.domain.recommend.service;

import com.navigator.knowledge.domain.recommend.messaging.dto.RecommendTaskRequestMessage;
import com.navigator.knowledge.domain.recommend.messaging.producer.RecommendTaskProducer;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.entity.TaskType;
import com.navigator.knowledge.domain.task.service.TaskService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import com.navigator.knowledge.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendTaskServiceTest {

    @Mock
    private TaskService taskService;

    @Mock
    private RecommendTaskProducer recommendTaskProducer;

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private UserService userService;

    @InjectMocks
    private RecommendTaskService recommendTaskService;

    @Test
    @DisplayName("추천 요청은 knowledge_tree를 포함한 메시지를 발행한다")
    void requestRecommendation_sendsKnowledgeTreeInMessage() {
        Long userId = 1L;
        Task task = Task.builder()
            .taskId("recommend-task-1")
            .userId(userId)
            .taskType(TaskType.RECOMMEND)
            .status(TaskStatus.PENDING)
            .expiresAt(LocalDateTime.now().plusSeconds(45))
            .build();
        Map<String, Map<String, List<String>>> knowledgeTree = Map.of(
            "Backend", Map.of("Database", List.of("PostgreSQL", "Redis"))
        );

        when(taskService.createTask(eq(userId), eq(null), eq(TaskType.RECOMMEND), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
            .thenReturn(task);
        when(knowledgeService.getKnowledgeTree(userId)).thenReturn(knowledgeTree);
        when(userService.getRequiredCareerGoal(userId)).thenReturn("Backend Developer");

        recommendTaskService.requestRecommendation(userId);

        ArgumentCaptor<RecommendTaskRequestMessage> messageCaptor = ArgumentCaptor.forClass(RecommendTaskRequestMessage.class);
        verify(recommendTaskProducer).sendTaskRequest(messageCaptor.capture());
        verify(taskService).updateTaskStatus(task.getTaskId(), TaskStatus.PROCESSING);

        RecommendTaskRequestMessage message = messageCaptor.getValue();
        assertThat(message.taskId()).isEqualTo(task.getTaskId());
        assertThat(message.userId()).isEqualTo(userId);
        assertThat(message.careerGoal()).isEqualTo("Backend Developer");
        assertThat(message.knowledgeTree()).isEqualTo(knowledgeTree);
        assertThat(message.expiredAt()).isNotBlank();
    }
}
