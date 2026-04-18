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
    name = "knowledge_keywords",
    uniqueConstraints = @UniqueConstraint(name = "uk_knowledge_keywords_topic_name", columnNames = {"topic_id", "name"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KnowledgeKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private KnowledgeTopic topic;

    @Column(nullable = false, length = 100)
    private String name;

    @Builder
    public KnowledgeKeyword(KnowledgeTopic topic, String name) {
        this.topic = topic;
        this.name = name;
    }
}
