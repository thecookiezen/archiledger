package com.thecookiezen.archiledger.application.service;

import com.thecookiezen.archiledger.domain.model.Entity;
import com.thecookiezen.archiledger.domain.model.EntityId;
import com.thecookiezen.archiledger.domain.model.EntityType;
import com.thecookiezen.archiledger.domain.model.Relation;
import com.thecookiezen.archiledger.domain.model.RelationType;
import com.thecookiezen.archiledger.domain.repository.EmbeddingsService;
import com.thecookiezen.archiledger.domain.repository.KnowledgeGraphRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceImplTest {

    @Mock
    private KnowledgeGraphRepository repository;

    @Mock
    private EmbeddingsService embeddingsService;

    @InjectMocks
    private KnowledgeGraphServiceImpl service;

    @Test
    void createEntities() {
        Entity entity = new Entity("test", "testType");
        when(repository.saveEntity(any(Entity.class))).thenReturn(entity);

        List<Entity> result = service.createEntities(List.of(entity));

        assertEquals(1, result.size());
        assertEquals("test", result.get(0).name().value());
        verify(repository).saveEntity(any(Entity.class));
    }

    @Test
    void createRelations() {
        Relation relation = new Relation("from", "to", "type");

        List<Relation> result = service.createRelations(List.of(relation));

        assertEquals(1, result.size());
        verify(repository).saveRelation(any(Relation.class));
    }

    @Test
    void readGraph() {
        Entity entity = new Entity("test", "testType");
        Relation relation = new Relation("from", "to", "type");
        when(repository.getGraph()).thenReturn(Map.of("entities", List.of(entity), "relations", List.of(relation)));

        Map<String, Object> graph = service.readGraph();

        assertNotNull(graph);
        assertEquals(1, ((List<?>) graph.get("entities")).size());
        assertEquals(1, ((List<?>) graph.get("relations")).size());
    }

    @Test
    void deleteEntities() {
        service.deleteEntities(List.of(new EntityId("test")));
        verify(repository).deleteEntity(new EntityId("test"));
    }

    @Test
    void getEntity_delegatesToRepository() {
        Entity entity = new Entity("myEntity", "Service");
        when(repository.findEntityById(new EntityId("myEntity"))).thenReturn(Optional.of(entity));

        Optional<Entity> result = service.getEntity(new EntityId("myEntity"));

        assertTrue(result.isPresent());
        assertEquals("myEntity", result.get().name().value());
        verify(repository).findEntityById(new EntityId("myEntity"));
    }

    @Test
    void getEntitiesByType_delegatesToRepository() {
        EntityType type = new EntityType("Service");
        List<Entity> entities = List.of(
                new Entity("Service1", "Service"),
                new Entity("Service2", "Service"));
        when(repository.findEntitiesByType(type)).thenReturn(entities);

        List<Entity> result = service.getEntitiesByType(type);

        assertEquals(2, result.size());
        verify(repository).findEntitiesByType(type);
    }

    @Test
    void getRelationsForEntity_delegatesToRepository() {
        EntityId entityId = new EntityId("myEntity");
        List<Relation> relations = List.of(
                new Relation("myEntity", "other", "CALLS"),
                new Relation("another", "myEntity", "DEPENDS_ON"));
        when(repository.findRelationsForEntity(entityId)).thenReturn(relations);

        List<Relation> result = service.getRelationsForEntity(entityId);

        assertEquals(2, result.size());
        verify(repository).findRelationsForEntity(entityId);
    }

    @Test
    void getRelationsByType_delegatesToRepository() {
        RelationType type = new RelationType("CALLS");
        List<Relation> relations = List.of(
                new Relation("A", "B", "CALLS"),
                new Relation("B", "C", "CALLS"));
        when(repository.findRelationsByType(type)).thenReturn(relations);

        List<Relation> result = service.getRelationsByType(type);

        assertEquals(2, result.size());
        verify(repository).findRelationsByType(type);
    }

    @Test
    void getRelatedEntities_delegatesToRepository() {
        EntityId entityId = new EntityId("A");
        List<Entity> entities = List.of(
                new Entity("B", "Node"),
                new Entity("C", "Node"));
        when(repository.findRelatedEntities(entityId)).thenReturn(entities);

        List<Entity> result = service.getRelatedEntities(entityId);

        assertEquals(2, result.size());
        verify(repository).findRelatedEntities(entityId);
    }

    @Test
    void getEntityTypes_delegatesToRepository() {
        Set<EntityType> types = Set.of(
                new EntityType("Service"),
                new EntityType("Database"));
        when(repository.findAllEntityTypes()).thenReturn(types);

        Set<EntityType> result = service.getEntityTypes();

        assertEquals(2, result.size());
        verify(repository).findAllEntityTypes();
    }

    @Test
    void getRelationTypes_delegatesToRepository() {
        Set<RelationType> types = Set.of(
                new RelationType("CALLS"),
                new RelationType("DEPENDS_ON"));
        when(repository.findAllRelationTypes()).thenReturn(types);

        Set<RelationType> result = service.getRelationTypes();

        assertEquals(2, result.size());
        verify(repository).findAllRelationTypes();
    }
}
