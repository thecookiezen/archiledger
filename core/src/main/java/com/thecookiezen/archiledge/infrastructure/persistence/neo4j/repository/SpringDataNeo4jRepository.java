package com.thecookiezen.archiledge.infrastructure.persistence.neo4j.repository;

import com.thecookiezen.archiledge.infrastructure.persistence.neo4j.model.Neo4jEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface SpringDataNeo4jRepository extends Neo4jRepository<Neo4jEntity, String> {

        @Query("MATCH (n:Entity) WHERE toLower(n.name) CONTAINS toLower($query) OR toLower(n.type) CONTAINS toLower($query) RETURN n")
        List<Neo4jEntity> searchEntities(String query);

        @Query("MATCH (n:Entity)-[r]->(m:Entity) RETURN n, r, m")
        List<Neo4jEntity> findAllEntitiesWithRelations();

        @Query("MATCH (n:Entity) WHERE n.type = $type RETURN n")
        List<Neo4jEntity> findByType(String type);

        @Query("MATCH (source:Entity)-[r:RELATED_TO]->(target:Entity) WHERE source.name = $entityName OR target.name = $entityName RETURN source.name AS fromName, target.name AS toName, r.relationType AS relationType")
        List<Map<String, String>> findRelationsForEntity(String entityName);

        @Query("MATCH (source:Entity)-[r:RELATED_TO]->(target:Entity) WHERE r.relationType = $relationType RETURN source.name AS fromName, target.name AS toName, r.relationType AS relationType")
        List<Map<String, String>> findRelationsByRelationType(String relationType);

        @Query("MATCH (n:Entity)-[r:RELATED_TO]-(m:Entity) WHERE n.name = $entityName RETURN DISTINCT m")
        List<Neo4jEntity> findRelatedEntities(String entityName);

        @Query("MATCH (n:Entity) RETURN DISTINCT n.type AS type")
        List<String> findAllEntityTypes();

        @Query("MATCH (:Entity)-[r:RELATED_TO]->(:Entity) RETURN DISTINCT r.relationType AS relationType")
        List<String> findAllRelationTypes();
}
