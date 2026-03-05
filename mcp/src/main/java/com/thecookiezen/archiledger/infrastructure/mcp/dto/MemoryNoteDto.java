package com.thecookiezen.archiledger.infrastructure.mcp.dto;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;

import java.time.Instant;
import java.util.List;

public record MemoryNoteDto(
        String id,
        String content,
        List<String> keywords,
        String context,
        List<String> tags,
        List<NoteLinkDto> links,
        String timestamp,
        int retrievalCount) {

    public MemoryNoteDto {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("MemoryNote id cannot be null or blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("MemoryNote content cannot be null");
        }
        keywords = (keywords != null) ? List.copyOf(keywords) : List.of();
        tags = (tags != null) ? List.copyOf(tags) : List.of();
        links = (links != null) ? List.copyOf(links) : List.of();
        if (timestamp == null || timestamp.isBlank()) {
            timestamp = Instant.now().toString();
        }
        if (retrievalCount < 0) {
            retrievalCount = 0;
        }
    }

    public MemoryNote toDomain() {
        return new MemoryNote(
                new MemoryNoteId(id),
                content,
                keywords,
                context,
                tags,
                links.stream().map(NoteLinkDto::toDomain).toList(),
                timestamp,
                retrievalCount,
                null);
    }

    public static MemoryNoteDto fromDomain(MemoryNote note) {
        return new MemoryNoteDto(
                note.id().value(),
                note.content(),
                note.keywords(),
                note.context(),
                note.tags(),
                note.links().stream().map(NoteLinkDto::fromDomain).toList(),
                note.timestamp(),
                note.retrievalCount());
    }
}
