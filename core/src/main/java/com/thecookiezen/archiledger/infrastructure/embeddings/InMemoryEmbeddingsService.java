package com.thecookiezen.archiledger.infrastructure.embeddings;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.thecookiezen.archiledger.domain.model.Entity;
import com.thecookiezen.archiledger.domain.repository.EmbeddingsService;

@Service
public class InMemoryEmbeddingsService implements EmbeddingsService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public InMemoryEmbeddingsService(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @Override
    public float[] generateEmbeddings(Entity entity) {
        float[] embeddings = embeddingModel.embed(entity.observationsJoined());
        vectorStore.add(List.of(
                new Document(entity.name().toString(), entity.observationsJoined(), Collections.emptyMap())));
        return embeddings;
    }

    @Override
    public List<String> findClosestMatch(String text) {
        return vectorStore.similaritySearch(text).stream()
                .map(Document::toString)
                .collect(Collectors.toList());
    }
}
