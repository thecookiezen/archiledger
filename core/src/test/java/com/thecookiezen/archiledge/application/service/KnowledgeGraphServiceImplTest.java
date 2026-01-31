package com.thecookiezen.archiledge.application.service;

import com.thecookiezen.archiledge.domain.model.Entity;
import com.thecookiezen.archiledge.domain.model.EntityId;
import com.thecookiezen.archiledge.domain.model.Relation;
import com.thecookiezen.archiledge.domain.repository.KnowledgeGraphRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceImplTest {

    @Mock
    private KnowledgeGraphRepository repository;

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
}
