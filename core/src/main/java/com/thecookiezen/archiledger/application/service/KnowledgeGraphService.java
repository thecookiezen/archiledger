package com.thecookiezen.archiledger.application.service;

import com.thecookiezen.archiledger.domain.model.Entity;
import com.thecookiezen.archiledger.domain.model.EntityId;
import com.thecookiezen.archiledger.domain.model.EntityType;
import com.thecookiezen.archiledger.domain.model.Relation;
import com.thecookiezen.archiledger.domain.model.RelationType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface KnowledgeGraphService {
    List<Entity> createEntities(List<Entity> newEntities);

    List<Relation> createRelations(List<Relation> newRelations);

    Map<String, Object> readGraph();

    void deleteEntities(List<EntityId> ids);

    void deleteRelations(List<Relation> relationsToDelete);

    Optional<Entity> getEntity(EntityId id);

    List<Entity> getEntitiesByType(EntityType type);

    List<Relation> getRelationsForEntity(EntityId entityId);

    List<Relation> getRelationsByType(RelationType type);

    List<Entity> getRelatedEntities(EntityId entityId);

    Set<EntityType> getEntityTypes();

    Set<RelationType> getRelationTypes();

    List<String> similaritySearch(String query);
}