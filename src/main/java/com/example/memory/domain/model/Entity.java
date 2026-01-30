package com.example.memory.domain.model;

import java.util.ArrayList;
import java.util.List;

public record Entity(String name, String type, List<String> observations) {
    public Entity(String name, String type) {
        this(name, type, new ArrayList<>());
    }
}
