package com.example.memory.infrastructure.persistence.neo4j.repository;

import com.example.memory.infrastructure.persistence.neo4j.model.Neo4jEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataNeo4jRepository extends Neo4jRepository<Neo4jEntity, String> {
    
    @Query("MATCH (n:Entity) WHERE toLower(n.name) CONTAINS toLower($query) OR toLower(n.type) CONTAINS toLower($query) RETURN n")
    List<Neo4jEntity> searchEntities(String query);
    
    @Query("MATCH (n:Entity)-[r]->(m:Entity) RETURN n, r, m")
    List<Neo4jEntity> findAllEntitiesWithRelations();
}
