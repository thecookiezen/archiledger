package com.thecookiezen.archiledger.application.service;

import com.thecookiezen.archiledger.domain.model.Entity;
import com.thecookiezen.archiledger.domain.model.EntityId;
import com.thecookiezen.archiledger.domain.model.EntityType;
import com.thecookiezen.archiledger.domain.model.Relation;
import com.thecookiezen.archiledger.domain.model.RelationType;
import com.thecookiezen.archiledger.domain.repository.KnowledgeGraphRepository;
import com.thecookiezen.archiledger.domain.repository.EmbeddingsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgeGraphRepository repository;
    private final EmbeddingsService embeddingsService;

    KnowledgeGraphServiceImpl(KnowledgeGraphRepository repository, EmbeddingsService embeddingsService) {
        this.repository = repository;
        this.embeddingsService = embeddingsService;
    }

    @Override
    public List<Entity> createEntities(List<Entity> newEntities) {
        List<Entity> created = new ArrayList<>();
        for (Entity e : newEntities) {
            created.add(repository.saveEntity(e));
            embeddingsService.generateEmbeddings(e);
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

    @Override
    public List<String> similaritySearch(String query) {
        return embeddingsService.findClosestMatch(query);
    }
}