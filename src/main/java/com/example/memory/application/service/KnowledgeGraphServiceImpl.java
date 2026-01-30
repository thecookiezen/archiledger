package com.example.memory.application.service;

import com.example.memory.domain.model.Entity;
import com.example.memory.domain.model.Relation;
import com.example.memory.domain.repository.KnowledgeGraphRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgeGraphRepository repository;

    public KnowledgeGraphServiceImpl(KnowledgeGraphRepository repository) {
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
    public List<Entity> searchNodes(String query) {
        return repository.searchEntities(query);
    }

    @Override
    public void deleteEntities(List<String> names) {
        for (String name : names) {
            repository.deleteEntity(name);
        }
    }

    @Override
    public void deleteRelations(List<Relation> relationsToDelete) {
        for (Relation r : relationsToDelete) {
            repository.deleteRelation(r);
        }
    }
}
