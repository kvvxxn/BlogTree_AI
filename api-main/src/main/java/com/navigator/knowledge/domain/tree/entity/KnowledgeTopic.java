package com.navigator.knowledge.domain.tree.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "knowledge_topics",
    uniqueConstraints = @UniqueConstraint(name = "uk_knowledge_topics_category_name", columnNames = {"category_id", "name"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KnowledgeTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private KnowledgeCategory category;

    @Column(nullable = false, length = 100)
    private String name;

    @Builder
    public KnowledgeTopic(KnowledgeCategory category, String name) {
        this.category = category;
        this.name = name;
    }
}
