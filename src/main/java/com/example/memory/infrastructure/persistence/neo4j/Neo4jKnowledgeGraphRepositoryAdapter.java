package com.example.memory.infrastructure.persistence.neo4j;

import com.example.memory.domain.model.Entity;
import com.example.memory.domain.model.Relation;
import com.example.memory.domain.repository.KnowledgeGraphRepository;
import com.example.memory.infrastructure.persistence.neo4j.model.Neo4jEntity;
import com.example.memory.infrastructure.persistence.neo4j.model.Neo4jRelationConnection;
import com.example.memory.infrastructure.persistence.neo4j.repository.SpringDataNeo4jRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Profile("neo4j")
public class Neo4jKnowledgeGraphRepositoryAdapter implements KnowledgeGraphRepository {

    private final SpringDataNeo4jRepository neo4jRepository;

    public Neo4jKnowledgeGraphRepositoryAdapter(SpringDataNeo4jRepository neo4jRepository) {
        this.neo4jRepository = neo4jRepository;
    }

    @Override
    @Transactional
    public Entity saveEntity(Entity entity) {
        Neo4jEntity neo4jEntity = neo4jRepository.findById(entity.name()).orElse(new Neo4jEntity(entity.name(), entity.type()));
        neo4jEntity.setType(entity.type()); // Update type if changed
        neo4jEntity.setObservations(entity.observations());
        
        Neo4jEntity saved = neo4jRepository.save(neo4jEntity);
        return toDomainEntity(saved);
    }

    @Override
    @Transactional
    public void saveRelation(Relation relation) {
        Neo4jEntity from = neo4jRepository.findById(relation.from())
                .orElseGet(() -> neo4jRepository.save(new Neo4jEntity(relation.from(), "unknown")));
        Neo4jEntity to = neo4jRepository.findById(relation.to())
                .orElseGet(() -> neo4jRepository.save(new Neo4jEntity(relation.to(), "unknown")));

        // Check if relation already exists to avoid duplication
        boolean exists = from.getRelations().stream()
                .anyMatch(r -> r.getTargetEntity().getName().equals(to.getName()) && r.getRelationType().equals(relation.relationType()));

        if (!exists) {
            from.getRelations().add(new Neo4jRelationConnection(to, relation.relationType()));
            neo4jRepository.save(from);
        }
    }

    @Override
    public List<Entity> findAllEntities() {
        return neo4jRepository.findAll().stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Relation> findAllRelations() {
        // This is a simplified fetch. For a large graph, this is inefficient.
        List<Relation> relations = new ArrayList<>();
        // Fetch entities with their relations eagerly if possible, otherwise this triggers lazy loading or multiple queries
        // In Neo4j SDN, we might need a custom query to get all relationships efficiently.
        // Using findAllEntitiesWithRelations from repository if configured, or just iterating (inefficient but OK for small graphs)
        List<Neo4jEntity> all = neo4jRepository.findAll(); 
        for (Neo4jEntity n : all) {
            if (n.getRelations() != null) {
                for (Neo4jRelationConnection rc : n.getRelations()) {
                    relations.add(new Relation(n.getName(), rc.getTargetEntity().getName(), rc.getRelationType()));
                }
            }
        }
        return relations;
    }

    @Override
    public Map<String, Object> getGraph() {
        return Map.of(
            "entities", findAllEntities(),
            "relations", findAllRelations()
        );
    }

    @Override
    public List<Entity> searchEntities(String query) {
        return neo4jRepository.searchEntities(query).stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteEntity(String name) {
        neo4jRepository.deleteById(name);
    }

    @Override
    @Transactional
    public void deleteRelation(Relation relation) {
        neo4jRepository.findById(relation.from()).ifPresent(from -> {
            from.getRelations().removeIf(r -> 
                r.getTargetEntity().getName().equals(relation.to()) && 
                r.getRelationType().equals(relation.relationType())
            );
            neo4jRepository.save(from);
        });
    }

    private Entity toDomainEntity(Neo4jEntity neo4jEntity) {
        return new Entity(neo4jEntity.getName(), neo4jEntity.getType(), neo4jEntity.getObservations());
    }
}
