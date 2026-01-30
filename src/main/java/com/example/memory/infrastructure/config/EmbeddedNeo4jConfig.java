package com.example.memory.infrastructure.config;

import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.file.Path;

@Configuration
@Profile("neo4j")
public class EmbeddedNeo4jConfig {

    @Value("${memory.neo4j.data-dir:}")
    private String dataDir;

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "spring.neo4j.uri", matchIfMissing = true, havingValue = "embedded")
    public Neo4j embeddedNeo4jServer() {
        var builder = (dataDir != null && !dataDir.isBlank())
                ? Neo4jBuilders.newInProcessBuilder(Path.of(dataDir))
                : Neo4jBuilders.newInProcessBuilder();

        return builder
                .withDisabledServer() // Disable HTTP server, we only need Bolt
                .withFixture("MERGE (n:Entity {name: 'Root', type: 'System'})")
                .build();
    }
}
