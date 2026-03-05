package com.thecookiezen.archiledger.domain.repository;

import com.thecookiezen.archiledger.domain.model.MemoryNote;

public interface EmbeddingsService {

    float[] generateEmbeddings(MemoryNote note);

    float[] embed(String text);
}