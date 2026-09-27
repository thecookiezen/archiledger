package com.thecookiezen.archiledger.infrastructure.embeddings;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.ladybugdb.QueryResult;
import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.vector.HnswOptions;
import com.thecookiezen.ladybugdb.spring.vector.VectorIndexOperations;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ensures the HNSW vector index on {@code MemoryNote.embedding} exists at
 * startup, using the managed {@link VectorIndexOperations} API from
 * spring-data-ladybugdb instead of hand-written {@code CALL ...} strings.
 * <p>
 * The vector extension is loaded per connection by the template machinery;
 * only the one-time {@code INSTALL vector} and the extension home directory
 * are handled here. Index creation is idempotent: if the index already exists,
 * startup is a no-op. For the periodic DROP + CREATE rebuild recommended for
 * write-heavy workloads, use {@link #recreateIndex()}.
 */
@Component
public class LadybugVectorExtensionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(LadybugVectorExtensionInitializer.class);

    private static final String VECTOR_INDEX_NAME = "note_embedding_idx";
    private static final String TABLE_NAME = "MemoryNote";
    private static final String EMBEDDING_PROPERTY = "embedding";

    private final Database database;
    private final VectorIndexOperations vectorIndexes;

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

    public LadybugVectorExtensionInitializer(Database database, LadybugDBTemplate template) {
        this.database = database;
        this.vectorIndexes = template.vectorIndexes();
    }

    @PostConstruct
    public void initialize() {
        installExtension();
        createVectorIndex();
    }

    /**
     * Periodic maintenance operation: rebuilds the index via DROP + CREATE in
     * one managed call. Dead HNSW edges accumulate after updates and deletes,
     * so write-heavy workloads should call this regularly.
     */
    public void recreateIndex() {
        logger.info("Rebuilding vector index '{}' on {}.{}", VECTOR_INDEX_NAME, TABLE_NAME, EMBEDDING_PROPERTY);
        vectorIndexes.rebuild(TABLE_NAME, VECTOR_INDEX_NAME, EMBEDDING_PROPERTY, hnswOptions());
        logger.info("Vector index '{}' rebuilt", VECTOR_INDEX_NAME);
    }

    private void installExtension() {
        try (Connection conn = new Connection(database)) {
            configureExtensionDirectory(conn);
            logger.info("Installing LadybugDB vector extension...");
            executeQuery(conn, "INSTALL vector");
            logger.info("Vector extension installed (or already present)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to install LadybugDB vector extension", e);
        }
    }

    private void createVectorIndex() {
        HnswOptions options = hnswOptions();
        logger.info("Ensuring HNSW vector index '{}' on {}.{} with mu={}, ml={}, pu={}, efc={}, metric={}",
                VECTOR_INDEX_NAME, TABLE_NAME, EMBEDDING_PROPERTY, options.mu(), options.ml(),
                options.pu(), options.efc(), options.metric());
        if (vectorIndexes.create(TABLE_NAME, VECTOR_INDEX_NAME, EMBEDDING_PROPERTY, options)) {
            logger.info("Vector index '{}' created", VECTOR_INDEX_NAME);
        } else {
            logger.info("Vector index '{}' already exists, skipping creation", VECTOR_INDEX_NAME);
        }
    }

    private HnswOptions hnswOptions() {
        return HnswOptions.builder()
                .mu(hnswMu)
                .ml(hnswMl)
                .pu(hnswPu)
                .metric(HnswOptions.Metric.from(hnswMetric))
                .efc(hnswEfc)
                .build();
    }

    private void configureExtensionDirectory(Connection conn) {
        if (extensionDir != null && !extensionDir.isBlank()) {
            logger.info("Configuring LadybugDB home directory for extensions: {}", extensionDir);
            executeQuery(conn, "CALL home_directory='" + extensionDir + "'");
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
