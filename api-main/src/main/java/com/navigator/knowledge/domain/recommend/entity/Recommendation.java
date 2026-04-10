package com.navigator.knowledge.domain.recommend.entity;

import com.navigator.knowledge.domain.task.entity.Task;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long recommendationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "task_id", unique = true, nullable = false)
    private Task task;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "category", length = 255, nullable = false)
    private String category;

    @Column(name = "topic", length = 255, nullable = false)
    private String topic;

    @Column(name = "keyword", length = 255, nullable = false)
    private String keyword;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Recommendation(Task task, Long userId, String reason, String category, String topic, String keyword) {
        this.task = task;
        this.userId = userId;
        this.reason = reason;
        this.category = category;
        this.topic = topic;
        this.keyword = keyword;
    }
}
