package com.navigator.knowledge.domain.task.service;

import com.navigator.knowledge.domain.task.exception.TaskNotFoundException;
import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.task.entity.TaskStatus;
import com.navigator.knowledge.domain.task.repository.TaskRepository;
import com.navigator.knowledge.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final String JPA_TRANSACTION_MANAGER = "jpaTransactionManager";
    private static final String TASK_EXPIRED_MESSAGE = ErrorCode.TASK_EXPIRED.getDefaultMessage();

    private final TaskRepository taskRepository;

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public Task createTask(Long userId, String sourceUrl, LocalDateTime expiresAt) {
        String taskId = UUID.randomUUID().toString();

        Task task = Task.builder()
                .taskId(taskId)
                .userId(userId)
                .sourceUrl(sourceUrl)
                .status(TaskStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
        
        return taskRepository.save(task);
    }

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public void updateTaskStatus(String taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        task.updateStatus(status);
    }

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public void updateTaskFailed(String taskId, String errorMessage) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        task.fail(errorMessage);
    }

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public boolean expireTask(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (task.getStatus().isTerminal()) {
            return false;
        }

        task.expire(TASK_EXPIRED_MESSAGE);
        return true;
    }

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public List<Task> expireTimedOutTasks(LocalDateTime now) {
        List<Task> tasks = taskRepository.findByStatusInAndExpiresAtBefore(
                List.of(TaskStatus.PENDING, TaskStatus.PROCESSING),
                now
        );

        tasks.forEach(task -> task.expire(TASK_EXPIRED_MESSAGE));
        return tasks;
    }

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER, readOnly = true)
    public Task getTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
