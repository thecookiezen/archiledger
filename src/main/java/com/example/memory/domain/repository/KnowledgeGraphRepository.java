package com.example.memory.domain.repository;

import com.example.memory.domain.model.Entity;
import com.example.memory.domain.model.Relation;

import java.util.List;
import java.util.Map;

public interface KnowledgeGraphRepository {
    Entity saveEntity(Entity entity);
    void saveRelation(Relation relation);
    List<Entity> findAllEntities();
    List<Relation> findAllRelations();
    Map<String, Object> getGraph();
    List<Entity> searchEntities(String query);
    void deleteEntity(String name);
    void deleteRelation(Relation relation);
}
