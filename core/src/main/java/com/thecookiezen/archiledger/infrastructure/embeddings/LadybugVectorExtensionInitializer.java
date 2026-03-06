package com.thecookiezen.archiledger.infrastructure.embeddings;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.ladybugdb.QueryResult;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ladybugdb")
public class LadybugVectorExtensionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(LadybugVectorExtensionInitializer.class);

    private static final String VECTOR_INDEX_NAME = "note_embedding_idx";
    private static final String TABLE_NAME = "NoteEmbedding";
    private static final String EMBEDDING_PROPERTY = "embedding";

    private final Database database;

    @Value("${ladybugdb.extension-dir:}")
    private String extensionDir;

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
        logger.info("Creating vector index '{}' on {}.{}...", VECTOR_INDEX_NAME, TABLE_NAME, EMBEDDING_PROPERTY);
        try (QueryResult result = conn.query(
                "CALL CREATE_VECTOR_INDEX('" + TABLE_NAME + "', '" + VECTOR_INDEX_NAME + "', '"
                        + EMBEDDING_PROPERTY + "', metric := 'cosine')")) {
            if (result.isSuccess()) {
                logger.info("Vector index '{}' created successfully", VECTOR_INDEX_NAME);
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
