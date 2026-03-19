package com.navigator.knowledge.domain.task.service;

import com.navigator.knowledge.domain.task.dto.TaskRequestDto;
import com.navigator.knowledge.domain.task.dto.TaskResponseDto;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponseDto createTask(TaskRequestDto request) {
        String taskId = UUID.randomUUID().toString();

        // todo: 임시로 사용할 userId (향후 인증 정보에서 가져오도록 수정)
        String userId = "test-user-id";

        // 2. Task 엔티티를 PENDING 상태로 DB에 저장
        Task task = Task.builder()
                .taskId(taskId)
                .userId(userId)
                .sourceUrl(request.sourceUrl())
                .status(TaskStatus.PENDING)
                .build();
        
        taskRepository.save(task);

        return new TaskResponseDto(taskId);
    }
}
