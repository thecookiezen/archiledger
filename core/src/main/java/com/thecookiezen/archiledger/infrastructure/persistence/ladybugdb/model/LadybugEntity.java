package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

import com.thecookiezen.ladybugdb.spring.annotation.NodeEntity;

@NodeEntity(label = "Entity")
public class LadybugEntity {
    @Id
    private String name;

    private String type;

    private List<String> observations = new ArrayList<>();

    public LadybugEntity() {
    }

    public LadybugEntity(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getObservations() {
        return observations;
    }

    public void setObservations(List<String> observations) {
        this.observations = observations;
    }
}
