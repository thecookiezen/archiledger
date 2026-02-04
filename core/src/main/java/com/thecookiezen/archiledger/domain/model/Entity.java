package com.thecookiezen.archiledger.domain.model;

import java.util.List;
import java.util.stream.Collectors;

public record Entity(EntityId name, EntityType type, List<String> observations) {
    public Entity {
        if (name == null) {
            throw new IllegalArgumentException("Entity name cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Entity type cannot be null");
        }
        observations = (observations != null) ? List.copyOf(observations) : List.of();
    }

    public Entity(String name, String type) {
        this(new EntityId(name), new EntityType(type), List.of());
    }

    public Entity(String name, String type, List<String> observations) {
        this(new EntityId(name), new EntityType(type), observations);
    }

    public String observationsJoined() {
        return observations.stream().collect(Collectors.joining());
    }
}
