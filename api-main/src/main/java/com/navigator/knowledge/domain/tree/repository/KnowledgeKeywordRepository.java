package com.navigator.knowledge.domain.tree.repository;

import com.navigator.knowledge.domain.tree.dto.KnowledgePathDto;
import com.navigator.knowledge.domain.tree.entity.KnowledgeKeyword;
import com.navigator.knowledge.domain.tree.entity.KnowledgeTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgeKeywordRepository extends JpaRepository<KnowledgeKeyword, Long> {

    Optional<KnowledgeKeyword> findByTopicAndName(KnowledgeTopic topic, String name);

    @Query("""
        select new com.navigator.knowledge.domain.tree.dto.KnowledgePathDto(c.name, t.name, k.name)
        from KnowledgeKeyword k
        join k.topic t
        join t.category c
        where c.userId = :userId
        order by c.name, t.name, k.name
        """)
    List<KnowledgePathDto> findAllKnowledgePathsByUserId(@Param("userId") Long userId);
}
