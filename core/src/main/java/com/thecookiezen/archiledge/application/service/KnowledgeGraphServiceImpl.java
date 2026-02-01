package com.thecookiezen.archiledge.application.service;

import com.thecookiezen.archiledge.domain.model.Entity;
import com.thecookiezen.archiledge.domain.model.EntityId;
import com.thecookiezen.archiledge.domain.model.EntityType;
import com.thecookiezen.archiledge.domain.model.Relation;
import com.thecookiezen.archiledge.domain.model.RelationType;
import com.thecookiezen.archiledge.domain.repository.KnowledgeGraphRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgeGraphRepository repository;

    KnowledgeGraphServiceImpl(KnowledgeGraphRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Entity> createEntities(List<Entity> newEntities) {
        List<Entity> created = new ArrayList<>();
        for (Entity e : newEntities) {
            created.add(repository.saveEntity(e));
        }
        return created;
    }

    @Override
    public List<Relation> createRelations(List<Relation> newRelations) {
        for (Relation r : newRelations) {
            repository.saveRelation(r);
        }
        return newRelations;
    }

    @Override
    public Map<String, Object> readGraph() {
        return repository.getGraph();
    }

    @Override
    public void deleteEntities(List<EntityId> ids) {
        for (EntityId id : ids) {
            repository.deleteEntity(id);
        }
    }

    @Override
    public void deleteRelations(List<Relation> relationsToDelete) {
        for (Relation r : relationsToDelete) {
            repository.deleteRelation(r);
        }
    }

    @Override
    public Optional<Entity> getEntity(EntityId id) {
        return repository.findEntityById(id);
    }

    @Override
    public List<Entity> getEntitiesByType(EntityType type) {
        return repository.findEntitiesByType(type);
    }

    @Override
    public List<Relation> getRelationsForEntity(EntityId entityId) {
        return repository.findRelationsForEntity(entityId);
    }

    @Override
    public List<Relation> getRelationsByType(RelationType type) {
        return repository.findRelationsByType(type);
    }

    @Override
    public List<Entity> getRelatedEntities(EntityId entityId) {
        return repository.findRelatedEntities(entityId);
    }

    @Override
    public Set<EntityType> getEntityTypes() {
        return repository.findAllEntityTypes();
    }

    @Override
    public Set<RelationType> getRelationTypes() {
        return repository.findAllRelationTypes();
    }
}