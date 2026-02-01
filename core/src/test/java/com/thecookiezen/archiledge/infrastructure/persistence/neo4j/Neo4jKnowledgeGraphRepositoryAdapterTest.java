package com.thecookiezen.archiledge.infrastructure.persistence.neo4j;

import com.thecookiezen.archiledge.domain.model.Entity;
import com.thecookiezen.archiledge.domain.model.EntityId;
import com.thecookiezen.archiledge.domain.model.EntityType;
import com.thecookiezen.archiledge.domain.model.Relation;
import com.thecookiezen.archiledge.domain.model.RelationType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.thecookiezen.archiledge.infrastructure.persistence.neo4j.repository.SpringDataNeo4jRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("neo4j")
@Testcontainers
class Neo4jKnowledgeGraphRepositoryAdapterTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.context.annotation.ComponentScan(basePackages = "com.thecookiezen.archiledge.infrastructure.persistence.neo4j")
    static class TestConfig {
    }

    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5")
            .withoutAuthentication();

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
    }

    @AfterAll
    static void tearDown() {
        if (neo4jContainer != null) {
            neo4jContainer.close();
        }
    }

    @Autowired
    private Neo4jKnowledgeGraphRepositoryAdapter repository;

    @Autowired
    private SpringDataNeo4jRepository neo4jRepository;

    @BeforeEach
    void cleanDatabase() {
        neo4jRepository.deleteAll();
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
}
