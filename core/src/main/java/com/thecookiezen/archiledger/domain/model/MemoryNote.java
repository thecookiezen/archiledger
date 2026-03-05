package com.thecookiezen.archiledger.domain.model;

import java.util.List;

public record MemoryNote(
        MemoryNoteId id,
        String content,
        List<String> keywords,
        String context,
        List<String> tags,
        List<NoteLink> links,
        String timestamp,
        int retrievalCount,
        float[] embedding) {

    public MemoryNote {
        if (id == null) {
            throw new IllegalArgumentException("MemoryNote id cannot be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("MemoryNote content cannot be null");
        }
        keywords = (keywords != null) ? List.copyOf(keywords) : List.of();
        tags = (tags != null) ? List.copyOf(tags) : List.of();
        links = (links != null) ? List.copyOf(links) : List.of();
        if (timestamp == null || timestamp.isBlank()) {
            throw new IllegalArgumentException("MemoryNote timestamp cannot be null or blank");
        }
        if (retrievalCount < 0) {
            throw new IllegalArgumentException("MemoryNote retrievalCount cannot be negative");
        }
    }

    public MemoryNote withRetrievalCount(int newCount) {
        return new MemoryNote(id, content, keywords, context, tags, links, timestamp, newCount, embedding);
    }

    public MemoryNote withLinks(List<NoteLink> newLinks) {
        return new MemoryNote(id, content, keywords, context, tags, newLinks, timestamp, retrievalCount, embedding);
    }

    public MemoryNote withEmbedding(float[] embedding) {
        return new MemoryNote(id, content, keywords, context, tags, links, timestamp, retrievalCount, embedding);
    }
}
