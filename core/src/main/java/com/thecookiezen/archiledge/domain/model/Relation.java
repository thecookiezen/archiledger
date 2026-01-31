package com.thecookiezen.archiledge.domain.model;

public record Relation(EntityId from, EntityId to, RelationType relationType) {
    public Relation {
        if (from == null) {
            throw new IllegalArgumentException("Relation from cannot be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Relation to cannot be null");
        }
        if (relationType == null) {
            throw new IllegalArgumentException("Relation type cannot be null");
        }
    }

    public Relation(String from, String to, String relationType) {
        this(new EntityId(from), new EntityId(to), new RelationType(relationType));
    }
}