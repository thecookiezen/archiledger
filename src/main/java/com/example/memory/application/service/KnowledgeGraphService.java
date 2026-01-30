package com.example.memory.application.service;

import com.example.memory.domain.model.Entity;
import com.example.memory.domain.model.Relation;

import java.util.List;
import java.util.Map;

public interface KnowledgeGraphService {
    List<Entity> createEntities(List<Entity> newEntities);
    List<Relation> createRelations(List<Relation> newRelations);
    Map<String, Object> readGraph();
    List<Entity> searchNodes(String query);
    void deleteEntities(List<String> names);
    void deleteRelations(List<Relation> relationsToDelete);
}
