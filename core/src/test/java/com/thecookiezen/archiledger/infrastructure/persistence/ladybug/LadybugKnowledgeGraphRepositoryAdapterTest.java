package com.thecookiezen.archiledger.infrastructure.persistence.ladybug;

import com.thecookiezen.archiledger.domain.model.Entity;
import com.thecookiezen.archiledger.domain.model.EntityId;
import com.thecookiezen.archiledger.domain.model.EntityType;
import com.thecookiezen.archiledger.domain.model.Relation;
import com.thecookiezen.archiledger.domain.model.RelationType;
import com.thecookiezen.archiledger.infrastructure.config.LadybugDBConfig;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.LadybugDbRepository;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.LadybugKnowledgeGraphRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LadybugKnowledgeGraphRepositoryAdapterTest.TestConfig.class)
@ActiveProfiles("ladybugdb")
class LadybugKnowledgeGraphRepositoryAdapterTest {

    @org.springframework.context.annotation.Configuration
    @org.springframework.context.annotation.Import(LadybugDBConfig.class)
    @org.springframework.context.annotation.ComponentScan(basePackages = {
            "com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb"
    })
    static class TestConfig {
    }

    @Autowired
    private LadybugKnowledgeGraphRepository repository;

    @Autowired
    private LadybugDbRepository ladybugDbRepository;

    @BeforeEach
    void cleanDatabase() {
        ladybugDbRepository.deleteAll();
    }

    @Test
    void saveAndFindEntity() {
        Entity entity = new Entity("IntegrationTest", "TestType");
        repository.saveEntity(entity);

        List<Entity> results = repository.findAllEntities();
        assertFalse(results.isEmpty());
        boolean found = results.stream().anyMatch(e -> e.name().equals(entity.name()));
        assertTrue(found);
    }

    @Test
    void saveAndFindRelation() {
        Entity sourceEntity = new Entity("SourceEntity", "TestType");
        Entity targetEntity = new Entity("TargetEntity", "TestType");
        repository.saveEntity(sourceEntity);
        repository.saveEntity(targetEntity);

        Relation relation = new Relation("SourceEntity", "TargetEntity", "TEST_RELATION");
        repository.saveRelation(relation);

        List<Relation> relations = repository.findAllRelations();
        boolean found = relations.stream()
                .anyMatch(r -> r.from().equals(new EntityId("SourceEntity"))
                        && r.to().equals(new EntityId("TargetEntity"))
                        && r.relationType().equals(new RelationType("TEST_RELATION")));
        assertTrue(found);
    }

    @Test
    void findEntityById_whenExists_returnsEntity() {
        repository.saveEntity(new Entity("MyService", "Service"));

        Optional<Entity> result = repository.findEntityById(new EntityId("MyService"));

        assertTrue(result.isPresent());
        assertEquals("MyService", result.get().name().value());
        assertEquals("Service", result.get().type().value());
    }

    @Test
    void findEntityById_whenNotExists_returnsEmpty() {
        Optional<Entity> result = repository.findEntityById(new EntityId("NonExistent"));

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

        repository.saveRelation(new Relation("A", "B", "CALLS"));
        repository.saveRelation(new Relation("C", "A", "DEPENDS_ON"));

        List<Relation> relationsForA = repository.findRelationsForEntity(new EntityId("A"));

        assertEquals(2, relationsForA.size());
    }

    @Test
    void findRelationsByType_returnsOnlyMatchingType() {
        repository.saveEntity(new Entity("A", "Node"));
        repository.saveEntity(new Entity("B", "Node"));
        repository.saveEntity(new Entity("C", "Node"));

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

        repository.saveRelation(new Relation("A", "B", "CALLS"));
        repository.saveRelation(new Relation("C", "A", "DEPENDS_ON"));
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
        repository.saveEntity(new Entity("A", "Node"));
        repository.saveEntity(new Entity("B", "Node"));
        repository.saveEntity(new Entity("C", "Node"));
        repository.saveEntity(new Entity("D", "Node"));

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

    @Test
    void deleteRelation_whenExists_deletesRelation() {
        repository.saveEntity(new Entity("A", "Node"));
        repository.saveEntity(new Entity("B", "Node"));
        repository.saveRelation(new Relation("A", "B", "CALLS"));

        repository.deleteRelation(new Relation("A", "B", "CALLS"));

        List<Relation> relations = repository.findAllRelations();
        assertTrue(relations.isEmpty());
    }

    @Test
    void deleteRelation_whenNotExists_doesNothing() {
        repository.saveEntity(new Entity("A", "Node"));
        repository.saveEntity(new Entity("B", "Node"));

        repository.deleteRelation(new Relation("A", "B", "CALLS"));

        List<Relation> relations = repository.findAllRelations();
        assertTrue(relations.isEmpty());
    }

    @Test
    void deleteEntity_whenExists_deletesEntity() {
        repository.saveEntity(new Entity("A", "Node"));

        repository.deleteEntity(new EntityId("A"));

        List<Entity> entities = repository.findAllEntities();
        assertTrue(entities.isEmpty());
    }

    @Test
    void deleteEntity_whenNotExists_doesNothing() {
        repository.deleteEntity(new EntityId("A"));

        List<Entity> entities = repository.findAllEntities();
        assertTrue(entities.isEmpty());
    }

    @Test
    void getGraph_returnsAllEntitiesAndRelations() {
        repository.saveEntity(new Entity("A", "Node"));
        repository.saveEntity(new Entity("B", "Node"));
        repository.saveRelation(new Relation("A", "B", "CALLS"));

        Map<String, Object> graph = repository.getGraph();

        List<Entity> entities = (List<Entity>) graph.get("entities");
        List<Relation> relations = (List<Relation>) graph.get("relations");

        assertEquals(2, entities.size());
        assertEquals(1, relations.size());
    }
}
