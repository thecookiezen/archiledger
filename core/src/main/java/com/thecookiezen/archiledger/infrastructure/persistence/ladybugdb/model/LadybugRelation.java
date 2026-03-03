package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model;

import org.springframework.data.annotation.Id;

import com.thecookiezen.ladybugdb.spring.annotation.RelationshipEntity;

@RelationshipEntity(type = "RELATED_TO", nodeType = LadybugEntity.class, sourceField = "sourceEntity", targetField = "targetEntity")
public class LadybugRelation {
    @Id
    private String id;

    private LadybugEntity sourceEntity;

    private LadybugEntity targetEntity;

    private String relationType;

    public LadybugRelation() {
    }

    public LadybugRelation(LadybugEntity sourceEntity, LadybugEntity targetEntity, String relationType) {
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.relationType = relationType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LadybugEntity getSourceEntity() {
        return sourceEntity;
    }

    public void setSourceEntity(LadybugEntity sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    public LadybugEntity getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(LadybugEntity targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
}
