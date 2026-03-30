package com.thecookiezen.archiledger.infrastructure.embeddings;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.ladybugdb.QueryResult;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LadybugVectorExtensionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(LadybugVectorExtensionInitializer.class);

    private static final String VECTOR_INDEX_NAME = "note_embedding_idx";
    private static final String TABLE_NAME = "NoteEmbedding";
    private static final String EMBEDDING_PROPERTY = "embedding";

    private final Database database;

    @Value("${ladybugdb.extension-dir:}")
    private String extensionDir;

    @Value("${ladybugdb.hnsw.mu:30}")
    private int hnswMu;

    @Value("${ladybugdb.hnsw.ml:60}")
    private int hnswMl;

    @Value("${ladybugdb.hnsw.pu:0.1}")
    private double hnswPu;

    @Value("${ladybugdb.hnsw.efc:300}")
    private int hnswEfc;

    @Value("${ladybugdb.hnsw.metric:cosine}")
    private String hnswMetric;

    public LadybugVectorExtensionInitializer(Database database) {
        this.database = database;
    }

    @PostConstruct
    public void initialize() {
        try (Connection conn = new Connection(database)) {
            configureExtensionDirectory(conn);
            installExtension(conn);
            loadExtension(conn);
            createVectorIndex(conn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize LadybugDB vector extension", e);
        }
    }

    public void recreateIndex() {
        try (Connection conn = new Connection(database)) {
            loadExtension(conn);
            createVectorIndex(conn);
        }
    }

    private void configureExtensionDirectory(Connection conn) {
        if (extensionDir != null && !extensionDir.isBlank()) {
            logger.info("Configuring LadybugDB home directory for extensions: {}", extensionDir);
            executeQuery(conn, "CALL home_directory='" + extensionDir + "'");
        }
    }

    private void installExtension(Connection conn) {
        logger.info("Installing LadybugDB vector extension...");
        executeQuery(conn, "INSTALL vector");
        logger.info("Vector extension installed (or already present)");
    }

    private void loadExtension(Connection conn) {
        logger.info("Loading LadybugDB vector extension...");
        executeQuery(conn, "LOAD vector");
        logger.info("Vector extension loaded");
    }

    private void createVectorIndex(Connection conn) {
        try {
            executeQuery(conn, "CALL DROP_VECTOR_INDEX('" + TABLE_NAME + "', '" + VECTOR_INDEX_NAME + "')");
            logger.info("Dropped existing vector index");
        } catch (Exception e) {
        }

        logger.info("Creating HNSW vector index '{}' on {}.{} with mu={}, ml={}, pu={}, efc={}, metric={}",
                VECTOR_INDEX_NAME, TABLE_NAME, EMBEDDING_PROPERTY, hnswMu, hnswMl, hnswPu, hnswEfc, hnswMetric);
        
        String indexQuery = String.format(
                "CALL CREATE_VECTOR_INDEX('%s', '%s', '%s', metric := '%s', mu := %d, ml := %d, pu := %.2f, efc := %d)",
                TABLE_NAME, VECTOR_INDEX_NAME, EMBEDDING_PROPERTY, hnswMetric, hnswMu, hnswMl, hnswPu, hnswEfc);
        
        try (QueryResult result = conn.query(indexQuery)) {
            if (result.isSuccess()) {
                logger.info("Vector index '{}' created successfully with HNSW parameters", VECTOR_INDEX_NAME);
            } else {
                String error = result.getErrorMessage();
                if (error != null && error.contains("already exists")) {
                    logger.info("Vector index '{}' already exists, skipping creation", VECTOR_INDEX_NAME);
                } else {
                    logger.warn("Failed to create vector index '{}': {}", VECTOR_INDEX_NAME, error);
                }
            }
        }
    }

    private void executeQuery(Connection conn, String cypher) {
        try (QueryResult result = conn.query(cypher)) {
            if (!result.isSuccess()) {
                throw new RuntimeException("Query failed: " + cypher + " — " + result.getErrorMessage());
            }
        }
    }
}
