package com.thecookiezen.archiledge.domain.repository;

import com.thecookiezen.archiledge.domain.model.Entity;
import com.thecookiezen.archiledge.domain.model.EntityId;
import com.thecookiezen.archiledge.domain.model.Relation;

import java.util.List;
import java.util.Map;

public interface KnowledgeGraphRepository {
    Entity saveEntity(Entity entity);
    void saveRelation(Relation relation);
    List<Entity> findAllEntities();
    List<Relation> findAllRelations();
    Map<String, Object> getGraph();
    List<Entity> searchEntities(String query);
    void deleteEntity(EntityId id);
    void deleteRelation(Relation relation);
}