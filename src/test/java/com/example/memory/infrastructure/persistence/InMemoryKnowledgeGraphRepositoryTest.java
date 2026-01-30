package com.example.memory.infrastructure.persistence;

import com.example.memory.domain.model.Entity;
import com.example.memory.domain.model.Relation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryKnowledgeGraphRepositoryTest {

    private InMemoryKnowledgeGraphRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryKnowledgeGraphRepository();
    }

    @Test
    void saveAndFindEntity() {
        Entity entity = new Entity("test", "concept");
        repository.saveEntity(entity);

        List<Entity> entities = repository.findAllEntities();
        assertEquals(1, entities.size());
        assertEquals("test", entities.get(0).name());
    }

    @Test
    void saveAndFindRelation() {
        Relation relation = new Relation("A", "B", "connects");
        repository.saveRelation(relation);

        List<Relation> relations = repository.findAllRelations();
        assertEquals(1, relations.size());
        assertEquals("connects", relations.get(0).relationType());
    }

    @Test
    void searchEntities() {
        repository.saveEntity(new Entity("Apple", "Fruit"));
        repository.saveEntity(new Entity("Banana", "Fruit"));
        repository.saveEntity(new Entity("Carrot", "Vegetable"));

        List<Entity> results = repository.searchEntities("Fruit");
        assertEquals(2, results.size());
    }

    @Test
    void deleteEntity() {
        repository.saveEntity(new Entity("test", "concept"));
        repository.saveRelation(new Relation("test", "other", "rel"));
        
        repository.deleteEntity("test");

        assertTrue(repository.findAllEntities().isEmpty());
        assertTrue(repository.findAllRelations().isEmpty());
    }
}
