package com.thecookiezen.archiledger.infrastructure.config;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugMemoryNote;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugNoteLink;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LinkProjection;
import com.thecookiezen.ladybugdb.spring.config.EnableLadybugDBRepositories;
import com.thecookiezen.ladybugdb.spring.connection.LadybugDBConnectionFactory;
import com.thecookiezen.ladybugdb.spring.connection.PooledConnectionFactory;
import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.mapper.EntityWriter;
import com.thecookiezen.ladybugdb.spring.mapper.RowMapper;
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
            initializeSchema(db);
        } else {
            Path dataDirPath = Path.of(dataDir);
            boolean isNewDatabase = !Files.exists(dataDirPath.resolve("data"));
            db = new Database(dataDir);
            if (isNewDatabase) {
                initializeSchema(db);
                logger.info("Creating new persistent LadybugDB database at: {}", dataDirPath.toAbsolutePath());
            } else {
                logger.info("Loaded existing LadybugDB database from: {}", dataDirPath.toAbsolutePath());
            }
        }

        return db;
    }

    private void initializeSchema(Database db) {
        try (Connection conn = new Connection(db)) {
            try (var r1 = conn.query(
                    "CREATE NODE TABLE IF NOT EXISTS MemoryNote(id STRING PRIMARY KEY, content STRING, keywords STRING[], context STRING, tags STRING[], timestamp STRING, retrievalCount INT64)")) {
                if (!r1.isSuccess()) {
                    throw new RuntimeException("Failed to create MemoryNote table: " + r1.getErrorMessage());
                }
                logger.info("MemoryNote node table ready");
            }
            try (var re = conn.query(
                    "CREATE NODE TABLE IF NOT EXISTS NoteEmbedding(noteId STRING PRIMARY KEY, embedding FLOAT[384])")) {
                if (!re.isSuccess()) {
                    throw new RuntimeException("Failed to create NoteEmbedding table: " + re.getErrorMessage());
                }
                logger.info("NoteEmbedding node table ready");
            }
            try (var rl = conn.query(
                    "CREATE REL TABLE IF NOT EXISTS HAS_EMBEDDING(FROM MemoryNote TO NoteEmbedding)")) {
                if (!rl.isSuccess()) {
                    throw new RuntimeException("Failed to create HAS_EMBEDDING table: " + rl.getErrorMessage());
                }
                logger.info("HAS_EMBEDDING relationship table ready");
            }
            try (var r2 = conn.query(
                    "CREATE REL TABLE IF NOT EXISTS LINKED_TO(FROM MemoryNote TO MemoryNote, name STRING, relationType STRING)")) {
                if (!r2.isSuccess()) {
                    throw new RuntimeException("Failed to create LINKED_TO table: " + r2.getErrorMessage());
                }
                logger.info("LINKED_TO relationship table ready");
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
    public LadybugDBTemplate ladybugDBTemplate(LadybugDBConnectionFactory connectionFactory, EntityRegistry registry) {
        return new LadybugDBTemplate(connectionFactory, registry);
    }

    @Bean
    public PlatformTransactionManager transactionManager(LadybugDBConnectionFactory connectionFactory) {
        return new LadybugDBTransactionManager(connectionFactory);
    }

    @Bean
    public EntityRegistry entityRegistry() {
        EntityRegistry registry = new EntityRegistry();
        registry.registerDescriptor(LadybugMemoryNote.class, memoryNoteReader(), memoryNoteWriter());
        registry.registerDescriptor(LadybugNoteLink.class, noteLinkReader(), noteLinkWriter());
        registry.registerDescriptor(LinkProjection.class, linkProjectionReader(), entity -> Map.of());
        registry.registerDescriptor(MemoryNoteId.class, memoryNoteIdReader(), entity -> Map.of());
        return registry;
    }

    private RowMapper<MemoryNoteId> memoryNoteIdReader() {
        return row -> {
            var id = row.getValue("id");
            return new MemoryNoteId(ValueMappers.asString(id));
        };
    }

    private RowMapper<LadybugMemoryNote> memoryNoteReader() {
        return row -> {
            var node = row.getNode("n");
            LadybugMemoryNote note = new LadybugMemoryNote();
            note.setId(ValueMappers.asString(node.get("id")));
            note.setContent(ValueMappers.asString(node.get("content")));
            note.setKeywords(ValueMappers.asStringList(node.get("keywords")));
            note.setContext(ValueMappers.asString(node.get("context")));
            note.setTags(ValueMappers.asStringList(node.get("tags")));
            note.setTimestamp(ValueMappers.asString(node.get("timestamp")));
            Integer retrievalCount = ValueMappers.asInteger(node.get("retrievalCount"));
            note.setRetrievalCount(retrievalCount != null ? retrievalCount : 0);
            return note;
        };
    }

    private EntityWriter<LadybugMemoryNote> memoryNoteWriter() {
        return note -> {
            Map<String, Object> props = new HashMap<>();
            props.put("content", note.getContent());
            props.put("keywords", note.getKeywords());
            props.put("context", note.getContext());
            props.put("tags", note.getTags());
            props.put("timestamp", note.getTimestamp());
            props.put("retrievalCount", note.getRetrievalCount());
            return props;
        };
    }

    private RowMapper<LadybugNoteLink> noteLinkReader() {
        return row -> {
            var rel = row.getRelationship("rel");
            var sourceNode = row.getNode("s");
            var targetNode = row.getNode("t");

            LadybugMemoryNote source = new LadybugMemoryNote();
            source.setId(ValueMappers.asString(sourceNode.get("id")));

            LadybugMemoryNote target = new LadybugMemoryNote();
            target.setId(ValueMappers.asString(targetNode.get("id")));

            String name = ValueMappers.asString(rel.properties().get("name"));
            return new LadybugNoteLink(name, source, target,
                    ValueMappers.asString(rel.properties().get("relationType")));
        };
    }

    private EntityWriter<LadybugNoteLink> noteLinkWriter() {
        return link -> {
            Map<String, Object> props = new HashMap<>();
            props.put("name", link.getName());
            props.put("relationType", link.getRelationType());
            return props;
        };
    }

    private RowMapper<LinkProjection> linkProjectionReader() {
        return row -> new LinkProjection(
                ValueMappers.asString(row.getValue("fromId")),
                ValueMappers.asString(row.getValue("toId")),
                ValueMappers.asString(row.getValue("relationType")));
    }
}
