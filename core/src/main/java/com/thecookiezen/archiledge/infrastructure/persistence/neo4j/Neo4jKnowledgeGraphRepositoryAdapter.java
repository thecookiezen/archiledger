package com.thecookiezen.archiledge.infrastructure.persistence.neo4j;

import com.thecookiezen.archiledge.domain.model.Entity;
import com.thecookiezen.archiledge.domain.model.EntityId;
import com.thecookiezen.archiledge.domain.model.EntityType;
import com.thecookiezen.archiledge.domain.model.Relation;
import com.thecookiezen.archiledge.domain.model.RelationType;
import com.thecookiezen.archiledge.domain.repository.KnowledgeGraphRepository;
import com.thecookiezen.archiledge.infrastructure.persistence.neo4j.model.Neo4jEntity;
import com.thecookiezen.archiledge.infrastructure.persistence.neo4j.model.Neo4jRelationConnection;
import com.thecookiezen.archiledge.infrastructure.persistence.neo4j.repository.SpringDataNeo4jRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Profile("neo4j")
class Neo4jKnowledgeGraphRepositoryAdapter implements KnowledgeGraphRepository {

    private final SpringDataNeo4jRepository neo4jRepository;

    Neo4jKnowledgeGraphRepositoryAdapter(SpringDataNeo4jRepository neo4jRepository) {
        this.neo4jRepository = neo4jRepository;
    }

    @Override
    @Transactional
    public Entity saveEntity(Entity entity) {
        Neo4jEntity neo4jEntity = neo4jRepository.findById(entity.name().value())
                .orElse(new Neo4jEntity(entity.name().value(), entity.type().value()));
        neo4jEntity.setType(entity.type().value());
        neo4jEntity.setObservations(entity.observations());

        Neo4jEntity saved = neo4jRepository.save(neo4jEntity);
        return toDomainEntity(saved);
    }

    @Override
    @Transactional
    public void saveRelation(Relation relation) {
        Neo4jEntity from = neo4jRepository.findById(relation.from().value())
                .orElseGet(() -> neo4jRepository.save(new Neo4jEntity(relation.from().value(), "unknown")));
        Neo4jEntity to = neo4jRepository.findById(relation.to().value())
                .orElseGet(() -> neo4jRepository.save(new Neo4jEntity(relation.to().value(), "unknown")));

        boolean exists = from.getRelations().stream()
                .anyMatch(r -> r.getTargetEntity().getName().equals(to.getName())
                        && r.getRelationType().equals(relation.relationType().value()));

        if (!exists) {
            from.getRelations().add(new Neo4jRelationConnection(to, relation.relationType().value()));
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
        List<Relation> relations = new ArrayList<>();
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
                "relations", findAllRelations());
    }

    @Override
    @Transactional
    public void deleteEntity(EntityId id) {
        neo4jRepository.deleteById(id.value());
    }

    @Override
    @Transactional
    public void deleteRelation(Relation relation) {
        neo4jRepository.findById(relation.from().value()).ifPresent(from -> {
            from.getRelations().removeIf(r -> r.getTargetEntity().getName().equals(relation.to().value()) &&
                    r.getRelationType().equals(relation.relationType().value()));
            neo4jRepository.save(from);
        });
    }

    private Entity toDomainEntity(Neo4jEntity neo4jEntity) {
        return new Entity(neo4jEntity.getName(), neo4jEntity.getType(), neo4jEntity.getObservations());
    }

    @Override
    public Optional<Entity> findEntityById(EntityId id) {
        return neo4jRepository.findById(id.value())
                .map(this::toDomainEntity);
    }

    @Override
    public List<Entity> findEntitiesByType(EntityType type) {
        return neo4jRepository.findByType(type.value()).stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Relation> findRelationsForEntity(EntityId entityId) {
        return neo4jRepository.findRelationsForEntity(entityId.value()).stream()
                .map(this::mapToRelation)
                .collect(Collectors.toList());
    }

    @Override
    public List<Relation> findRelationsByType(RelationType type) {
        return neo4jRepository.findRelationsByRelationType(type.value()).stream()
                .map(this::mapToRelation)
                .collect(Collectors.toList());
    }

    @Override
    public List<Entity> findRelatedEntities(EntityId entityId) {
        return neo4jRepository.findRelatedEntities(entityId.value()).stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Set<EntityType> findAllEntityTypes() {
        return neo4jRepository.findAllEntityTypes().stream()
                .filter(type -> type != null && !type.isBlank())
                .map(EntityType::new)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RelationType> findAllRelationTypes() {
        return neo4jRepository.findAllRelationTypes().stream()
                .filter(type -> type != null && !type.isBlank())
                .map(RelationType::new)
                .collect(Collectors.toSet());
    }

    private Relation mapToRelation(java.util.Map<String, String> map) {
        return new Relation(map.get("fromName"), map.get("toName"), map.get("relationType"));
    }
}