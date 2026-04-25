package com.navigator.knowledge.domain.tree.repository;

import com.navigator.knowledge.domain.tree.entity.KnowledgeCategory;
import com.navigator.knowledge.domain.tree.entity.KnowledgeTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeTopicRepository extends JpaRepository<KnowledgeTopic, Long> {

    Optional<KnowledgeTopic> findByCategoryAndName(KnowledgeCategory category, String name);
}
