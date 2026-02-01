package com.thecookiezen.archiledge.infrastructure.persistence.neo4j.repository;

public record RelationProjection(String fromName, String toName, String relationType) {
}
