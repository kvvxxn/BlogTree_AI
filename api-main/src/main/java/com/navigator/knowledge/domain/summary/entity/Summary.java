package com.navigator.knowledge.domain.summary.entity;

import com.navigator.knowledge.domain.task.entity.Task;
import com.navigator.knowledge.domain.tree.entity.KnowledgeKeyword;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "task_id", unique = true, nullable = false)
    private Task task;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id")
    private KnowledgeKeyword keyword;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Summary(Task task, Long userId, String sourceUrl, String content) {
        this.task = task;
        this.userId = userId;
        this.sourceUrl = sourceUrl;
        this.content = content;
    }

    public void assignKeyword(KnowledgeKeyword keyword) {
        this.keyword = keyword;
    }
}
