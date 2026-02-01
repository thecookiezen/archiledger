package com.thecookiezen.archiledge.infrastructure.persistence;

import com.thecookiezen.archiledge.domain.model.Entity;
import com.thecookiezen.archiledge.domain.model.EntityId;
import com.thecookiezen.archiledge.domain.model.EntityType;
import com.thecookiezen.archiledge.domain.model.Relation;
import com.thecookiezen.archiledge.domain.model.RelationType;
import com.thecookiezen.archiledge.domain.repository.KnowledgeGraphRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@Profile("default")
class InMemoryKnowledgeGraphRepository implements KnowledgeGraphRepository {
    private final Map<EntityId, Entity> entities = new ConcurrentHashMap<>();
    private final List<Relation> relations = new ArrayList<>();

    @Override
    public Entity saveEntity(Entity entity) {
        entities.put(entity.name(), entity);
        return entity;
    }

    @Override
    public void saveRelation(Relation relation) {
        relations.add(relation);
    }

    @Override
    public List<Entity> findAllEntities() {
        return new ArrayList<>(entities.values());
    }

    @Override
    public List<Relation> findAllRelations() {
        return new ArrayList<>(relations);
    }

    @Override
    public Map<String, Object> getGraph() {
        return Map.of(
                "entities", new ArrayList<>(entities.values()),
                "relations", new ArrayList<>(relations));
    }

    @Override
    public void deleteEntity(EntityId id) {
        entities.remove(id);
        relations.removeIf(r -> r.from().equals(id) || r.to().equals(id));
    }

    @Override
    public void deleteRelation(Relation relation) {
        relations.remove(relation);
    }

    @Override
    public Optional<Entity> findEntityById(EntityId id) {
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public List<Entity> findEntitiesByType(EntityType type) {
        return entities.values().stream()
                .filter(e -> e.type().equals(type))
                .collect(Collectors.toList());
    }

    @Override
    public List<Relation> findRelationsForEntity(EntityId entityId) {
        return relations.stream()
                .filter(r -> r.from().equals(entityId) || r.to().equals(entityId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Relation> findRelationsByType(RelationType type) {
        return relations.stream()
                .filter(r -> r.relationType().equals(type))
                .collect(Collectors.toList());
    }

    @Override
    public List<Entity> findRelatedEntities(EntityId entityId) {
        Set<EntityId> relatedIds = new HashSet<>();
        for (Relation r : relations) {
            if (r.from().equals(entityId)) {
                relatedIds.add(r.to());
            } else if (r.to().equals(entityId)) {
                relatedIds.add(r.from());
            }
        }
        return relatedIds.stream()
                .map(entities::get)
                .filter(e -> e != null)
                .collect(Collectors.toList());
    }

    @Override
    public Set<EntityType> findAllEntityTypes() {
        return entities.values().stream()
                .map(Entity::type)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RelationType> findAllRelationTypes() {
        return relations.stream()
                .map(Relation::relationType)
                .collect(Collectors.toSet());
    }
}
