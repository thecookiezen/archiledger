package com.example.memory.infrastructure.persistence.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("Entity")
public class Neo4jEntity {

    @Id
    private String name;

    private String type;

    private List<String> observations = new ArrayList<>();

    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.OUTGOING)
    private List<Neo4jRelationConnection> relations = new ArrayList<>();

    public Neo4jEntity() {}

    public Neo4jEntity(String name, String type) {
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

    public List<Neo4jRelationConnection> getRelations() {
        return relations;
    }

    public void setRelations(List<Neo4jRelationConnection> relations) {
        this.relations = relations;
    }
}
