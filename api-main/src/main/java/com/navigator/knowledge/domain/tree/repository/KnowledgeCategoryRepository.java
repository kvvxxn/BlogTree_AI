package com.navigator.knowledge.domain.tree.repository;

import com.navigator.knowledge.domain.tree.entity.KnowledgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategory, Long> {

    Optional<KnowledgeCategory> findByUserIdAndName(Long userId, String name);
}
