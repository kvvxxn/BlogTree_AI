package com.navigator.knowledge.domain.task.service;

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
    public Task createTask(String userId, String sourceUrl) {
        String taskId = UUID.randomUUID().toString();

        Task task = Task.builder()
                .taskId(taskId)
                .userId(userId)
                .sourceUrl(sourceUrl)
                .status(TaskStatus.PENDING)
                .build();
        
        return taskRepository.save(task);
    }

    @Transactional
    public void updateTaskStatus(String taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        task.updateStatus(status);
    }

    @Transactional
    public void updateTaskFailed(String taskId, String errorMessage) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        task.fail(errorMessage);
    }
}