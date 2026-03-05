package com.thecookiezen.archiledger.infrastructure.embeddings;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.repository.EmbeddingsService;

@Service
public class LadybugEmbeddingsService implements EmbeddingsService {

    private final EmbeddingModel embeddingModel;

    public LadybugEmbeddingsService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] generateEmbeddings(MemoryNote note) {
        return embeddingModel.embed(note.content());
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}
