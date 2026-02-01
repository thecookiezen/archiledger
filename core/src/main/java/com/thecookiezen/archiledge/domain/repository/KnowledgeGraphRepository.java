package com.thecookiezen.archiledge.domain.repository;

import com.thecookiezen.archiledge.domain.model.Entity;
import com.thecookiezen.archiledge.domain.model.EntityId;
import com.thecookiezen.archiledge.domain.model.EntityType;
import com.thecookiezen.archiledge.domain.model.Relation;
import com.thecookiezen.archiledge.domain.model.RelationType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface KnowledgeGraphRepository {
    Entity saveEntity(Entity entity);

    void saveRelation(Relation relation);

    List<Entity> findAllEntities();

    List<Relation> findAllRelations();

    Map<String, Object> getGraph();

    void deleteEntity(EntityId id);

    void deleteRelation(Relation relation);

    Optional<Entity> findEntityById(EntityId id);

    List<Entity> findEntitiesByType(EntityType type);

    List<Relation> findRelationsForEntity(EntityId entityId);

    List<Relation> findRelationsByType(RelationType type);

    List<Entity> findRelatedEntities(EntityId entityId);

    Set<EntityType> findAllEntityTypes();

    Set<RelationType> findAllRelationTypes();
}