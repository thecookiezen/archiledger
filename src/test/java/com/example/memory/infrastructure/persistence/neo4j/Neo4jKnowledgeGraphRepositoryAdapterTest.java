package com.example.memory.infrastructure.persistence.neo4j;

import com.example.memory.domain.model.Entity;
import com.example.memory.domain.model.Relation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("neo4j")
@ContextConfiguration(initializers = Neo4jKnowledgeGraphRepositoryAdapterTest.Neo4jInitializer.class)
@Transactional
class Neo4jKnowledgeGraphRepositoryAdapterTest {

    private static Neo4j embeddedDatabaseServer;

    static class Neo4jInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                    .withDisabledServer()
                    .build();

            TestPropertyValues.of(
                    "spring.neo4j.uri=" + embeddedDatabaseServer.boltURI().toString(),
                    "spring.neo4j.authentication.username=neo4j",
                    "spring.neo4j.authentication.password=password"
            ).applyTo(applicationContext.getEnvironment());
        }
    }

    @AfterAll
    static void stopNeo4j() {
        if (embeddedDatabaseServer != null) {
            embeddedDatabaseServer.close();
        }
    }

    @Autowired
    private Neo4jKnowledgeGraphRepositoryAdapter repository;

    @Test
    void saveAndFindEntity() {
        Entity entity = new Entity("IntegrationTest", "TestType");
        repository.saveEntity(entity);

        List<Entity> results = repository.findAllEntities();
        assertFalse(results.isEmpty());
        boolean found = results.stream().anyMatch(e -> e.name().equals("IntegrationTest"));
        assert(found);
    }

    @Test
    void saveAndFindRelation() {
        Entity e1 = new Entity("N1", "T");
        Entity e2 = new Entity("N2", "T");
        repository.saveEntity(e1);
        repository.saveEntity(e2);

        Relation relation = new Relation("N1", "N2", "TEST_REL");
        repository.saveRelation(relation);

        List<Relation> relations = repository.findAllRelations();
        boolean found = relations.stream()
                .anyMatch(r -> r.from().equals("N1") && r.to().equals("N2") && r.relationType().equals("TEST_REL"));
        assert(found);
    }
}
