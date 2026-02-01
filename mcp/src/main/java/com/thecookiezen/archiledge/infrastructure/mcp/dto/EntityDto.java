package com.thecookiezen.archiledge.infrastructure.mcp.dto;

import com.thecookiezen.archiledge.domain.model.Entity;

import java.util.List;

public record EntityDto(
        String name,
        String type,
        List<String> observations) {
    public EntityDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Entity name cannot be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Entity type cannot be null or blank");
        }
        observations = (observations != null) ? List.copyOf(observations) : List.of();
    }

    public EntityDto(String name, String type) {
        this(name, type, List.of());
    }

    public Entity toDomain() {
        return new Entity(name, type, observations);
    }

    public static EntityDto fromDomain(Entity entity) {
        return new EntityDto(
                entity.name().value(),
                entity.type().value(),
                entity.observations());
    }
}
