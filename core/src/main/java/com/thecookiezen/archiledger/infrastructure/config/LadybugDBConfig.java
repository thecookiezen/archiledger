package com.thecookiezen.archiledger.infrastructure.config;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugEntity;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugRelation;
import com.thecookiezen.ladybugdb.spring.config.EnableLadybugDBRepositories;
import com.thecookiezen.ladybugdb.spring.connection.LadybugDBConnectionFactory;
import com.thecookiezen.ladybugdb.spring.connection.PooledConnectionFactory;
import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.mapper.ValueMappers;
import com.thecookiezen.ladybugdb.spring.repository.support.EntityRegistry;
import com.thecookiezen.ladybugdb.spring.transaction.LadybugDBTransactionManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Profile("ladybugdb")
@EnableTransactionManagement
@EnableLadybugDBRepositories(basePackages = "com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb")
public class LadybugDBConfig {

    private static final Logger logger = LoggerFactory.getLogger(LadybugDBConfig.class);

    @Value("${ladybugdb.pool.max-total:10}")
    private int poolMaxTotal;

    @Value("${ladybugdb.pool.max-idle:5}")
    private int poolMaxIdle;

    @Value("${ladybugdb.pool.min-idle:2}")
    private int poolMinIdle;

    @Value("${ladybugdb.data-dir:}")
    private String dataDir;

    @Bean(destroyMethod = "close")
    public Database database() {
        Database db;
        if (dataDir == null || dataDir.isBlank()) {
            logger.info("No data directory configured, creating in-memory LadybugDB database");
            db = new Database(":memory:");
        } else {
            Path dataDirPath = Path.of(dataDir);
            boolean isNewDatabase = !Files.exists(dataDirPath.resolve("data"));

            if (isNewDatabase) {
                logger.info("Creating new persistent LadybugDB database at: {}", dataDirPath.toAbsolutePath());
            } else {
                logger.info("Loaded existing LadybugDB database from: {}", dataDirPath.toAbsolutePath());
            }

            db = new Database(dataDir);
        }

        initializeSchema(db);
        return db;
    }

    private void initializeSchema(Database db) {
        try (Connection conn = new Connection(db)) {
            try (var r1 = conn.query(
                    "CREATE NODE TABLE IF NOT EXISTS Entity(name STRING PRIMARY KEY, type STRING, observations STRING[])")) {
                if (!r1.isSuccess()) {
                    throw new RuntimeException("Failed to create Entity table: " + r1.getErrorMessage());
                }
                logger.info("Entity node table ready");
            }
            try (var r2 = conn.query(
                    "CREATE REL TABLE IF NOT EXISTS RELATED_TO(FROM Entity TO Entity, relationType STRING)")) {
                if (!r2.isSuccess()) {
                    throw new RuntimeException("Failed to create RELATED_TO table: " + r2.getErrorMessage());
                }
                logger.info("RELATED_TO relationship table ready");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    @Bean(destroyMethod = "close")
    public LadybugDBConnectionFactory connectionFactory(Database database) {
        return new PooledConnectionFactory(database);
    }

    @Bean
    public LadybugDBTemplate ladybugDBTemplate(LadybugDBConnectionFactory connectionFactory) {
        return new LadybugDBTemplate(connectionFactory);
    }

    @Bean
    public PlatformTransactionManager transactionManager(LadybugDBConnectionFactory connectionFactory) {
        return new LadybugDBTransactionManager(connectionFactory);
    }

    @Bean
    public EntityRegistry entityRegistry() {
        EntityRegistry registry = new EntityRegistry();
        registry.registerDescriptor(LadybugEntity.class, entityReader(), entityWriter());
        registry.registerDescriptor(LadybugRelation.class, relationReader(), relationWriter());
        return registry;
    }

    private com.thecookiezen.ladybugdb.spring.mapper.RowMapper<LadybugEntity> entityReader() {
        return row -> {
            var node = row.getNode("n");
            LadybugEntity entity = new LadybugEntity();
            entity.setName(ValueMappers.asString(node.get("name")));
            entity.setType(ValueMappers.asString(node.get("type")));
            entity.setObservations(ValueMappers.asStringList(node.get("observations")));
            return entity;
        };
    }

    private com.thecookiezen.ladybugdb.spring.mapper.EntityWriter<LadybugEntity> entityWriter() {
        return entity -> {
            Map<String, Object> props = new HashMap<>();
            props.put("type", entity.getType());
            props.put("observations", entity.getObservations());
            return props;
        };
    }

    private com.thecookiezen.ladybugdb.spring.mapper.RowMapper<LadybugRelation> relationReader() {
        return row -> {
            var rel = row.getRelationship("rel");
            var sourceNode = row.getNode("s");
            var targetNode = row.getNode("t");

            LadybugEntity source = new LadybugEntity();
            source.setName(ValueMappers.asString(sourceNode.get("name")));
            source.setType(ValueMappers.asString(sourceNode.get("type")));

            LadybugEntity target = new LadybugEntity();
            target.setName(ValueMappers.asString(targetNode.get("name")));
            target.setType(ValueMappers.asString(targetNode.get("type")));

            LadybugRelation relation = new LadybugRelation(source, target,
                    ValueMappers.asString(rel.properties().get("relationType")));
            relation.setId(String.valueOf(rel.id()));
            return relation;
        };
    }

    private com.thecookiezen.ladybugdb.spring.mapper.EntityWriter<LadybugRelation> relationWriter() {
        return relation -> {
            Map<String, Object> props = new HashMap<>();
            props.put("relationType", relation.getRelationType());
            return props;
        };
    }
}
