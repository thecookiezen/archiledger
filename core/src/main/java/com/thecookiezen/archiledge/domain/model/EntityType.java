package com.thecookiezen.archiledge.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record EntityType(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public EntityType {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EntityType cannot be null or blank");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}