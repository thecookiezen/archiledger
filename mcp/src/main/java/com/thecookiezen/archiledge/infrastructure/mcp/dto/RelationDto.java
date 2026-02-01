package com.thecookiezen.archiledge.infrastructure.mcp.dto;

import com.thecookiezen.archiledge.domain.model.Relation;

public record RelationDto(
        String from,
        String to,
        String relationType) {
    public RelationDto {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Relation from cannot be null or blank");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Relation to cannot be null or blank");
        }
        if (relationType == null || relationType.isBlank()) {
            throw new IllegalArgumentException("Relation type cannot be null or blank");
        }
    }

    public Relation toDomain() {
        return new Relation(from, to, relationType);
    }

    public static RelationDto fromDomain(Relation relation) {
        return new RelationDto(
                relation.from().value(),
                relation.to().value(),
                relation.relationType().value());
    }
}
