package com.example.memory.infrastructure.persistence.neo4j.model;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class Neo4jRelationConnection {

    @RelationshipId
    private String id;

    @TargetNode
    private Neo4jEntity targetEntity;

    private String relationType;

    public Neo4jRelationConnection() {}

    public Neo4jRelationConnection(Neo4jEntity targetEntity, String relationType) {
        this.targetEntity = targetEntity;
        this.relationType = relationType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Neo4jEntity getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(Neo4jEntity targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
}
