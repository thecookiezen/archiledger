package com.thecookiezen.archiledge.infrastructure.persistence;

import com.thecookiezen.archiledge.domain.model.Entity;
import com.thecookiezen.archiledge.domain.model.EntityId;
import com.thecookiezen.archiledge.domain.model.EntityType;
import com.thecookiezen.archiledge.domain.model.Relation;
import com.thecookiezen.archiledge.domain.model.RelationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        assertEquals("test", entities.get(0).name().value());
    }

    @Test
    void saveAndFindRelation() {
        Relation relation = new Relation("A", "B", "connects");
        repository.saveRelation(relation);

        List<Relation> relations = repository.findAllRelations();
        assertEquals(1, relations.size());
        assertEquals("connects", relations.get(0).relationType().value());
    }

    @Test
    void deleteEntity() {
        repository.saveEntity(new Entity("test", "concept"));
        repository.saveRelation(new Relation("test", "other", "rel"));

        repository.deleteEntity(new EntityId("test"));

        assertTrue(repository.findAllEntities().isEmpty());
        assertTrue(repository.findAllRelations().isEmpty());
    }

    @Test
    void findEntityById_whenExists_returnsEntity() {
        Entity entity = new Entity("myEntity", "Service");
        repository.saveEntity(entity);

        Optional<Entity> result = repository.findEntityById(new EntityId("myEntity"));

        assertTrue(result.isPresent());
        assertEquals("myEntity", result.get().name().value());
        assertEquals("Service", result.get().type().value());
    }

    @Test
    void findEntityById_whenNotExists_returnsEmpty() {
        Optional<Entity> result = repository.findEntityById(new EntityId("nonexistent"));

        assertTrue(result.isEmpty());
    }

    @Test
    void findEntitiesByType_returnsOnlyMatchingTypes() {
        repository.saveEntity(new Entity("UserService", "Service"));
        repository.saveEntity(new Entity("OrderService", "Service"));
        repository.saveEntity(new Entity("UserDB", "Database"));

        List<Entity> services = repository.findEntitiesByType(new EntityType("Service"));
        List<Entity> databases = repository.findEntitiesByType(new EntityType("Database"));

        assertEquals(2, services.size());
        assertTrue(services.stream().allMatch(e -> e.type().value().equals("Service")));

        assertEquals(1, databases.size());
        assertEquals("UserDB", databases.get(0).name().value());
    }

    @Test
    void findRelationsForEntity_returnsBothIncomingAndOutgoing() {
        repository.saveEntity(new Entity("A", "Node"));
        repository.saveEntity(new Entity("B", "Node"));
        repository.saveEntity(new Entity("C", "Node"));

        repository.saveRelation(new Relation("A", "B", "CALLS")); // A -> B
        repository.saveRelation(new Relation("C", "A", "DEPENDS_ON")); // C -> A

        List<Relation> relationsForA = repository.findRelationsForEntity(new EntityId("A"));

        assertEquals(2, relationsForA.size());
    }

    @Test
    void findRelationsByType_returnsOnlyMatchingType() {
        repository.saveRelation(new Relation("A", "B", "CALLS"));
        repository.saveRelation(new Relation("B", "C", "CALLS"));
        repository.saveRelation(new Relation("A", "C", "DEPENDS_ON"));

        List<Relation> callsRelations = repository.findRelationsByType(new RelationType("CALLS"));
        List<Relation> dependsRelations = repository.findRelationsByType(new RelationType("DEPENDS_ON"));

        assertEquals(2, callsRelations.size());
        assertTrue(callsRelations.stream().allMatch(r -> r.relationType().value().equals("CALLS")));

        assertEquals(1, dependsRelations.size());
    }

    @Test
    void findRelatedEntities_returnsAllConnectedEntities() {
        repository.saveEntity(new Entity("A", "Node"));
        repository.saveEntity(new Entity("B", "Node"));
        repository.saveEntity(new Entity("C", "Node"));
        repository.saveEntity(new Entity("D", "Node"));

        repository.saveRelation(new Relation("A", "B", "CALLS")); // A -> B
        repository.saveRelation(new Relation("C", "A", "DEPENDS_ON")); // C -> A
        // D is not connected to A

        List<Entity> relatedToA = repository.findRelatedEntities(new EntityId("A"));

        assertEquals(2, relatedToA.size());
        Set<String> names = Set.of(
                relatedToA.get(0).name().value(),
                relatedToA.get(1).name().value());
        assertTrue(names.contains("B"));
        assertTrue(names.contains("C"));
        assertFalse(names.contains("D"));
    }

    @Test
    void findAllEntityTypes_returnsUniqueTypes() {
        repository.saveEntity(new Entity("Service1", "Service"));
        repository.saveEntity(new Entity("Service2", "Service"));
        repository.saveEntity(new Entity("DB1", "Database"));
        repository.saveEntity(new Entity("API1", "API"));

        Set<EntityType> types = repository.findAllEntityTypes();

        assertEquals(3, types.size());
        assertTrue(types.contains(new EntityType("Service")));
        assertTrue(types.contains(new EntityType("Database")));
        assertTrue(types.contains(new EntityType("API")));
    }

    @Test
    void findAllRelationTypes_returnsUniqueTypes() {
        repository.saveRelation(new Relation("A", "B", "CALLS"));
        repository.saveRelation(new Relation("B", "C", "CALLS"));
        repository.saveRelation(new Relation("A", "C", "DEPENDS_ON"));
        repository.saveRelation(new Relation("C", "D", "USES"));

        Set<RelationType> types = repository.findAllRelationTypes();

        assertEquals(3, types.size());
        assertTrue(types.contains(new RelationType("CALLS")));
        assertTrue(types.contains(new RelationType("DEPENDS_ON")));
        assertTrue(types.contains(new RelationType("USES")));
    }

    @Test
    void findAllEntityTypes_whenEmpty_returnsEmptySet() {
        Set<EntityType> types = repository.findAllEntityTypes();
        assertTrue(types.isEmpty());
    }

    @Test
    void findAllRelationTypes_whenEmpty_returnsEmptySet() {
        Set<RelationType> types = repository.findAllRelationTypes();
        assertTrue(types.isEmpty());
    }
}
