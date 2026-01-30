package com.example.memory.infrastructure.persistence;

import com.example.memory.domain.model.Entity;
import com.example.memory.domain.model.Relation;
import com.example.memory.domain.repository.KnowledgeGraphRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@Profile("default")
public class InMemoryKnowledgeGraphRepository implements KnowledgeGraphRepository {
    private final Map<String, Entity> entities = new ConcurrentHashMap<>();
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
            "relations", new ArrayList<>(relations)
        );
    }

    @Override
    public List<Entity> searchEntities(String query) {
        return entities.values().stream()
                .filter(e -> e.name().toLowerCase().contains(query.toLowerCase()) || 
                             e.type().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteEntity(String name) {
        entities.remove(name);
        relations.removeIf(r -> r.from().equals(name) || r.to().equals(name));
    }

    @Override
    public void deleteRelation(Relation relation) {
        relations.remove(relation);
    }
}
