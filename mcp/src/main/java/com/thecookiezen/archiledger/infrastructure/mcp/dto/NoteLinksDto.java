package com.thecookiezen.archiledger.infrastructure.mcp.dto;

import java.util.List;

public record NoteLinksDto(String fromNoteId, List<NoteLinkDto> links) {
    public NoteLinksDto {
        if (fromNoteId == null || fromNoteId.isBlank()) {
            throw new IllegalArgumentException("NoteLinksDto fromNoteId cannot be null or blank");
        }
        if (links == null) {
            throw new IllegalArgumentException("NoteLinksDto links cannot be null");
        }
    }
}
