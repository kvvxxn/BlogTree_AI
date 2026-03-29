package com.navigator.knowledge.domain.tree.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("User")
public class UserNode {
    @Id
    private Long userId;

    @Relationship(type = "OWNS_CATEGORY", direction = Relationship.Direction.OUTGOING)
    private Set<Category> categories = new HashSet<>();

    public void addCategory(Category category) {
        categories.add(category);
    }
}
