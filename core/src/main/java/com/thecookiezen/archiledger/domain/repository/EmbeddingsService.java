package com.thecookiezen.archiledger.domain.repository;

import java.util.List;

import com.thecookiezen.archiledger.domain.model.Entity;

public interface EmbeddingsService {

    public void generateEmbeddings(Entity entity);

    public List<String> findClosestMatch(String text);

    public void deleteEmbeddings(List<String> idList);
}