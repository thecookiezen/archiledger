package com.thecookiezen.archiledger.infrastructure.persistence.ladybug;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.SimilarityResult;
import com.thecookiezen.archiledger.domain.repository.EmbeddingsService;
import com.thecookiezen.archiledger.infrastructure.config.LadybugDBConfig;
import com.thecookiezen.archiledger.infrastructure.embeddings.LadybugVectorExtensionInitializer;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.LadybugMemoryNoteRepository;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.MemoryNoteDbRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SimilaritySearchIntegrationTest.TestConfig.class)
class SimilaritySearchIntegrationTest {

    @Configuration
    @Import(LadybugDBConfig.class)
    @ComponentScan(basePackages = {
            "com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb",
            "com.thecookiezen.archiledger.infrastructure.embeddings"
    })
    static class TestConfig {

        @Bean
        public EmbeddingModel embeddingModel() {
            return new TransformersEmbeddingModel();
        }

    }

    @Autowired
    private LadybugMemoryNoteRepository repository;

    @Autowired
    private MemoryNoteDbRepository dbRepository;

    @Autowired
    private EmbeddingsService embeddingsService;

    @Autowired
    private LadybugVectorExtensionInitializer vectorExtensionInitializer;

    @BeforeEach
    void cleanDatabase() {
        dbRepository.deleteAllNotesWithEmbeddings();
        vectorExtensionInitializer.recreateIndex();
    }

    @Test
    void similaritySearch_higherScoreMeansBetterMatch() {
        MemoryNote javaNote = createNote("java-note", 
                "Java is a high-level programming language. Spring Boot is a popular Java framework for building microservices.");
        MemoryNote cookingNote = createNote("cooking-note",
                "To make pasta, boil water and add salt. Cook spaghetti for 10 minutes until al dente.");
        MemoryNote pythonNote = createNote("python-note",
                "Python is a high-level programming language. Django is a popular Python framework for web development.");
        MemoryNote gardeningNote = createNote("gardening-note",
                "Plant tomatoes in spring. Water them regularly and provide plenty of sunlight.");

        saveNoteWithEmbedding(javaNote);
        saveNoteWithEmbedding(cookingNote);
        saveNoteWithEmbedding(pythonNote);
        saveNoteWithEmbedding(gardeningNote);

        float[] queryEmbedding = embeddingsService.embed("programming languages and software development");
        List<SimilarityResult<MemoryNote>> results = repository.findSimilar(queryEmbedding, 10);

        assertEquals(4, results.size());

        double javaScore = findScoreForNote(results, "java-note");
        double pythonScore = findScoreForNote(results, "python-note");
        double cookingScore = findScoreForNote(results, "cooking-note");
        double gardeningScore = findScoreForNote(results, "gardening-note");

        assertTrue(javaScore > cookingScore, 
                "Java note (score=" + javaScore + ") should score higher than cooking note (score=" + cookingScore + ")");
        assertTrue(pythonScore > cookingScore, 
                "Python note (score=" + pythonScore + ") should score higher than cooking note (score=" + cookingScore + ")");
        assertTrue(javaScore > gardeningScore, 
                "Java note (score=" + javaScore + ") should score higher than gardening note (score=" + gardeningScore + ")");
        assertTrue(pythonScore > gardeningScore, 
                "Python note (score=" + pythonScore + ") should score higher than gardening note (score=" + gardeningScore + ")");

        assertTrue(javaScore > 0.3, "Java note should have a reasonably high score (> 0.3), got: " + javaScore);
        assertTrue(pythonScore > 0.3, "Python note should have a reasonably high score (> 0.3), got: " + pythonScore);
        assertTrue(cookingScore < 0.5, "Cooking note should have a lower score (< 0.5), got: " + cookingScore);
        assertTrue(gardeningScore < 0.5, "Gardening note should have a lower score (< 0.5), got: " + gardeningScore);
    }

    @Test
    void similaritySearch_semanticallySimilarContent_scoresHigherThanUnrelated() {
        MemoryNote architectureNote = createNote("arch-note",
                "The system uses a microservices architecture with event-driven communication between services.");
        MemoryNote distributedNote = createNote("distributed-note",
                "Distributed systems require careful consideration of consistency, availability, and partition tolerance.");
        MemoryNote recipeNote = createNote("recipe-note",
                "Chocolate cake recipe: mix flour, sugar, eggs, cocoa powder, and butter. Bake at 180C for 30 minutes.");

        saveNoteWithEmbedding(architectureNote);
        saveNoteWithEmbedding(distributedNote);
        saveNoteWithEmbedding(recipeNote);

        float[] queryEmbedding = embeddingsService.embed("software architecture patterns");
        List<SimilarityResult<MemoryNote>> results = repository.findSimilar(queryEmbedding, 10, -1, 0);

        assertEquals(3, results.size());

        double archScore = findScoreForNote(results, "arch-note");
        double distributedScore = findScoreForNote(results, "distributed-note");
        double recipeScore = findScoreForNote(results, "recipe-note");

        assertTrue(archScore > recipeScore, 
                "Architecture note (score=" + archScore + ") should score higher than recipe (score=" + recipeScore + ")");
        assertTrue(distributedScore > recipeScore, 
                "Distributed systems note (score=" + distributedScore + ") should score higher than recipe (score=" + recipeScore + ")");

        String firstNoteId = results.get(0).item().id().value();
        assertTrue(firstNoteId.equals("arch-note") || firstNoteId.equals("distributed-note"),
                "Top result should be either architecture or distributed note, much recipe. Got: " + firstNoteId);
    }

    @Test
    void similaritySearch_resultsAreOrderedByScoreDescending() {
        MemoryNote note1 = createNote("note-1", "REST API design with HTTP methods GET POST PUT DELETE");
        MemoryNote note2 = createNote("note-2", "GraphQL provides flexible query language for APIs");
        MemoryNote note3 = createNote("note-3", "The weather today is sunny with temperatures around 25 degrees");

        saveNoteWithEmbedding(note1);
        saveNoteWithEmbedding(note2);
        saveNoteWithEmbedding(note3);

        float[] queryEmbedding = embeddingsService.embed("building web APIs");
        List<SimilarityResult<MemoryNote>> results = repository.findSimilar(queryEmbedding, 10);

        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).score() >= results.get(i +1).score(),
                    "Results should be ordered by score descending. " +
                    "Result " + i + " (score=" + results.get(i).score() + ") should be >= " +
                    "result " + (i+1) + " (score=" + results.get(i+1).score() + ")");
        }
    }

    @Test
    void similaritySearch_withThreshold_filtersOutLowScoringResults() {
        MemoryNote javaNote = createNote("java-note", 
                "Java is a high-level programming language for enterprise applications.");
        MemoryNote cookingNote = createNote("cooking-note", "Pasta recipes and cooking techniques");
        MemoryNote pythonNote = createNote("python-note", "Python is a high-level programming language. Django is a popular Python framework.");
        MemoryNote gardeningNote = createNote("gardening-note", "Gardening tips and plant care");

        saveNoteWithEmbedding(javaNote);
        saveNoteWithEmbedding(cookingNote);
        saveNoteWithEmbedding(pythonNote);
        saveNoteWithEmbedding(gardeningNote);

        float[] queryEmbedding = embeddingsService.embed("programming languages and software development");
        List<SimilarityResult<MemoryNote>> results = repository.findSimilar(queryEmbedding, 10, 0.3, 0.0);

        assertEquals(2, results.size());
        double javaScore = findScoreForNote(results, "java-note");
        double pythonScore = findScoreForNote(results, "python-note");
        assertTrue(javaScore > pythonScore, 
                "Java note should score higher than Python note");
    }

    @Test
    void similaritySearch_withTemperature_exponentialScaling() {
        MemoryNote javaNote = createNote("java-note", 
                "Java is a high-level programming language for Spring Boot is a popular Java framework.");
        MemoryNote pythonNote = createNote("python-note", 
                "Python is a high-level programming language. Django is a popular Python framework.");
        MemoryNote cookingNote = createNote("cooking-note", "Pasta recipes and cooking techniques");
        MemoryNote unrelatedNote = createNote("unrelated", "Chocolate cake recipe: mix flour, sugar, eggs, cocoa powder, and butter. Bake at 180C for 30 minutes.");

        saveNoteWithEmbedding(javaNote);
        saveNoteWithEmbedding(pythonNote);
        saveNoteWithEmbedding(cookingNote);
        saveNoteWithEmbedding(unrelatedNote);

        float[] queryEmbedding = embeddingsService.embed("programming languages and software development");
        
        double temperature = 0.5;
        List<SimilarityResult<MemoryNote>> results = repository.findSimilar(queryEmbedding, 10, -1, temperature);

        
        assertEquals(4, results.size());

        double pythonScore = findScoreForNote(results, "python-note");
        double cookingScore = findScoreForNote(results, "cooking-note");
        double unrelatedScore = findScoreForNote(results, "unrelated");

        assertTrue(pythonScore > cookingScore && cookingScore > unrelatedScore,
                "Java should score highest, followed by Python");
        assertTrue(unrelatedScore < cookingScore, "Unrelated note should score lower than cooking note");
        assertTrue(unrelatedScore < pythonScore, "Unrelated note should score lower than Python note");
    }

    @Test
    void similaritySearch_withTemperatureAndThreshold_filtersByScore() {
        MemoryNote javaNote = createNote("java-note", 
                "Java is a high-level programming language. Spring Boot is a popular Java framework.");
        MemoryNote pythonNote = createNote("python-note", 
                "Python is a high-level programming language. Django is a popular Python framework.");
        MemoryNote cookingNote = createNote("cooking-note", "Pasta recipes and cooking techniques");
        MemoryNote unrelatedNote = createNote("unrelated", "Chocolate cake recipe: mix flour, sugar, eggs, cocoa powder, and butter. Bake at 180C for 30 minutes.");

        saveNoteWithEmbedding(javaNote);
        saveNoteWithEmbedding(pythonNote);
        saveNoteWithEmbedding(cookingNote);
        saveNoteWithEmbedding(unrelatedNote);

        float[] queryEmbedding = embeddingsService.embed("programming languages and software development");
        double temperature = 0.5;
        double threshold = 0.3;
        List<SimilarityResult<MemoryNote>> results = repository.findSimilar(queryEmbedding, 10, threshold, temperature);

        
        assertTrue(results.size() == 2, "Should return at least 2 results with threshold 0.3");

        double javaScore = findScoreForNote(results, "java-note");
        double pythonScore = findScoreForNote(results, "python-note");

        assertTrue(javaScore >= threshold, "Java note should pass threshold");
        assertTrue(pythonScore >= threshold, "Python note should pass threshold");
    }

    @Test
    void similaritySearch_higherTemperatureGivesHigherScoresForDistantMatches() {
        MemoryNote note1 = createNote("note-1", "Java Spring Boot microservices REST API");
        MemoryNote note2 = createNote("note-2", "Python Django web framework GraphQL");
        MemoryNote note3 = createNote("note-3", "Chocolate cake baking recipe oven temperature");
        
        saveNoteWithEmbedding(note1);
        saveNoteWithEmbedding(note2);
        saveNoteWithEmbedding(note3);

        float[] queryEmbedding = embeddingsService.embed("software development frameworks");
        
        double lowTempScore = findScoreForNote(repository.findSimilar(queryEmbedding, 10, 0.0, 0.1), "note-1");
        double highTempScore = findScoreForNote(repository.findSimilar(queryEmbedding, 10, 0.0, 1.0), "note-1");
        
        assertTrue(lowTempScore < highTempScore, 
                "Lower temperature should give lower score for same distance");
    }

    private MemoryNote createNote(String id, String content) {
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
    private void saveNoteWithEmbedding(MemoryNote note) {
        float[] embedding = embeddingsService.generateEmbeddings(note);
        repository.save(note.withEmbedding(embedding));
    }
    private double findScoreForNote(List<SimilarityResult<MemoryNote>> results, String noteId) {
        return results.stream()
                .filter(r -> r.item().id().value().equals(noteId))
                .findFirst()
                .map(SimilarityResult::score)
                .orElseThrow(() -> new AssertionError("Note not found: " + noteId));
    }
}
