package com.thecookiezen.archiledge.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record RelationType(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public RelationType {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RelationType cannot be null or blank");
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