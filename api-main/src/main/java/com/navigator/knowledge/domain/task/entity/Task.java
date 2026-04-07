package com.navigator.knowledge.domain.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @Column(name = "task_id", length = 36)
    private String taskId;

    @Column(name = "user_id", length = 36)
    private Long userId;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskStatus status; // PENDING, SUCCESS, PARTIAL_SUCCESS, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Task(String taskId, Long userId, String sourceUrl, TaskStatus status, LocalDateTime expiresAt) {
        this.taskId = taskId;
        this.userId = userId;
        this.sourceUrl = sourceUrl;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public void updateStatus(TaskStatus status) {
        this.status = status;
        if (status.isTerminal()) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void fail(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void expire(String errorMessage) {
        this.status = TaskStatus.EXPIRED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return expiresAt.isBefore(now) || expiresAt.isEqual(now);
    }
}
