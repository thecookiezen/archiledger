package com.thecookiezen.archiledger.infrastructure.persistence.ladybug;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.SimilarityResult;
import com.thecookiezen.archiledger.infrastructure.config.LadybugDBConfig;
import com.thecookiezen.archiledger.infrastructure.embeddings.LadybugVectorExtensionInitializer;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.LadybugMemoryNoteRepository;
import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for save-time updates of the indexed note embedding.
 * <p>
 * Saving an existing note must SET the vector on the live HNSW index
 * (supported since LadybugDB 0.18.0) instead of delete+recreating the
 * {@code NoteEmbedding} node: the new vector must immediately be visible
 * to KNN search, the old position must disappear, repeated updates must
 * never surface stale rows, and everything must survive a checkpoint and
 * reopen.
 * <p>
 * Uses hand-crafted orthonormal vectors under the default cosine metric:
 * identical vectors have distance ~0, orthogonal vectors distance ~1, so
 * positions can be asserted without a real embedding model.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EmbeddingUpdateIntegrationTest.TestConfig.class)
@TestMethodOrder(OrderAnnotation.class)
class EmbeddingUpdateIntegrationTest {

    private static final int DIMENSIONS = 384;

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void ladybugProperties(DynamicPropertyRegistry registry) {
        registry.add("ladybugdb.data-path", () -> dataDir.resolve("test-db").toString());
    }

    @Configuration
    @Import({LadybugDBConfig.class, LadybugVectorExtensionInitializer.class})
    @ComponentScan(basePackages = "com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb")
    static class TestConfig {
    }

    @Autowired
    private LadybugMemoryNoteRepository repository;

    @Autowired
    private LadybugDBTemplate template;

    @Test
    @Order(1)
    @DirtiesContext
    void update_movesNoteToNewPositionAndDropsTheOldOne() {
        MemoryNote note = note("update-note", "Original content about Java programming");
        repository.save(note.withEmbedding(basis(0)));

        assertScore("update-note", basis(0), 0.99, Double.MAX_VALUE);

        repository.save(note("update-note", "Updated content about cooking pasta").withEmbedding(basis(1)));

        // The note must be found at its new position...
        assertScore("update-note", basis(1), 0.99, Double.MAX_VALUE);
        // ...and no longer at the old one (orthogonal vectors have cosine distance ~1).
        assertScore("update-note", basis(0), -1.0, 0.05);

        // Flush the WAL so the reopened database in the next test method sees
        // the update even without relying on close-time checkpointing.
        template.execute("CHECKPOINT");
    }

    @Test
    @Order(2)
    void update_survivesCheckpointAndReopen() {
        // Fresh context: the previous method was @DirtiesContext, so the
        // database was closed and is now reopened from the same data directory.
        assertScore("update-note", basis(1), 0.99, Double.MAX_VALUE);
        assertScore("update-note", basis(0), -1.0, 0.05);

        MemoryNote reopened = repository.findById(new MemoryNoteId("update-note")).orElseThrow();
        assertEquals("Updated content about cooking pasta", reopened.content());
    }

    @Test
    @Order(3)
    void repeatedUpdates_doNotReturnStaleRows() {
        repository.save(note("r1", "REST API design with HTTP methods").withEmbedding(basis(10)));
        repository.save(note("r2", "GraphQL flexible query language").withEmbedding(basis(11)));
        repository.save(note("r3", "Chocolate cake baking recipe").withEmbedding(basis(12)));

        repository.save(note("r2", "GraphQL flexible query language").withEmbedding(basis(13)));
        repository.save(note("r2", "GraphQL flexible query language").withEmbedding(basis(14)));
        repository.save(note("r2", "GraphQL flexible query language").withEmbedding(basis(15)));

        List<SimilarityResult<MemoryNote>> results = repository.findSimilar(basis(15), 10);

        assertDistinctNotes(results);
        assertScore("r2", basis(15), 0.99, Double.MAX_VALUE);
    }

    @Test
    @Order(4)
    void saveWithUnchangedEmbedding_keepsResultsStable() {
        // Re-saving with the identical vector takes the skip-write path; the
        // index must be unchanged afterwards.
        repository.save(note("r2", "GraphQL flexible query language").withEmbedding(basis(15)));

        List<SimilarityResult<MemoryNote>> results = repository.findSimilar(basis(15), 10);

        assertDistinctNotes(results);
        assertScore("r2", basis(15), 0.99, Double.MAX_VALUE);
    }

    /** Every hit must be a distinct note — a duplicate id would mean a stale row from an old vector version. */
    private void assertDistinctNotes(List<SimilarityResult<MemoryNote>> results) {
        Set<String> ids = results.stream().map(r -> r.item().id().value()).collect(Collectors.toSet());
        assertEquals(results.size(), ids.size(), "duplicate ids in KNN results: " + ids);
        assertTrue(ids.containsAll(Set.of("r1", "r2", "r3")), "expected all notes, got: " + ids);
    }

    /**
     * Orthonormal basis vector: 1.0 at {@code index}, 0.0 elsewhere. Under
     * cosine distance, two distinct basis vectors are at distance ~1, a
     * vector from itself at distance ~0.
     */
    private static float[] basis(int index) {
        float[] vector = new float[DIMENSIONS];
        vector[index] = 1.0f;
        return vector;
    }

    private MemoryNote note(String id, String content) {
        return new MemoryNote(
                new MemoryNoteId(id),
                content,
                List.of(),
                "test-context",
                List.of("test"),
                List.of(),
                "2026-03-21T10:00:00Z",
                0,
                null);
    }

    private void assertScore(String noteId, float[] query, double minInclusive, double maxInclusive) {
        List<SimilarityResult<MemoryNote>> results = repository.findSimilar(query, 10);
        double score = results.stream()
                .filter(r -> r.item().id().value().equals(noteId))
                .findFirst()
                .map(SimilarityResult::score)
                .orElseThrow(() -> new AssertionError("Note not found in KNN results: " + noteId));
        assertTrue(score >= minInclusive && score <= maxInclusive,
                "note '" + noteId + "' score=" + score + " expected in [" + minInclusive + ", " + maxInclusive + "]");
    }
}
