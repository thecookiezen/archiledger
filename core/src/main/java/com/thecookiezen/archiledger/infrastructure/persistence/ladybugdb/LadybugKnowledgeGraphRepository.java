package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb;

import com.thecookiezen.archiledger.domain.model.Entity;
import com.thecookiezen.archiledger.domain.model.EntityId;
import com.thecookiezen.archiledger.domain.model.EntityType;
import com.thecookiezen.archiledger.domain.model.Relation;
import com.thecookiezen.archiledger.domain.model.RelationType;
import com.thecookiezen.archiledger.domain.repository.KnowledgeGraphRepository;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugEntity;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugRelation;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.RelationProjection;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@Profile("ladybugdb")
public class LadybugKnowledgeGraphRepository implements KnowledgeGraphRepository {

    private final LadybugDbRepository ladybugDbRepository;

    public LadybugKnowledgeGraphRepository(LadybugDbRepository ladybugDbRepository) {
        this.ladybugDbRepository = ladybugDbRepository;
    }

    @Override
    public Entity saveEntity(Entity entity) {
        LadybugEntity ladybugEntity = ladybugDbRepository.findById(entity.name().value())
                .orElse(new LadybugEntity(entity.name().value(), entity.type().value()));
        ladybugEntity.setType(entity.type().value());
        ladybugEntity.setObservations(entity.observations());

        LadybugEntity saved = ladybugDbRepository.save(ladybugEntity);
        return toDomainEntity(saved);
    }

    @Override
    public void saveRelation(Relation relation) {
        LadybugEntity from = ladybugDbRepository.findById(relation.from().value())
                .orElseGet(() -> ladybugDbRepository.save(new LadybugEntity(relation.from().value(), "unknown")));
        LadybugEntity to = ladybugDbRepository.findById(relation.to().value())
                .orElseGet(() -> ladybugDbRepository.save(new LadybugEntity(relation.to().value(), "unknown")));

        boolean exists = ladybugDbRepository.findRelationsBySource(from).stream()
                .anyMatch(r -> r.getTargetEntity().getName().equals(to.getName())
                        && r.getRelationType().equals(relation.relationType().value()));

        if (!exists) {
            LadybugRelation ladybugRelation = new LadybugRelation(from, to, relation.relationType().value());
            ladybugDbRepository.createRelation(from, to, ladybugRelation);
        }
    }

    @Override
    public List<Entity> findAllEntities() {
        return StreamSupport.stream(ladybugDbRepository.findAll().spliterator(), false)
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Relation> findAllRelations() {
        return ladybugDbRepository.findAllRelations().stream()
                .map(this::mapLadybugRelationToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getGraph() {
        return Map.of(
                "entities", findAllEntities(),
                "relations", findAllRelations());
    }

    @Override
    public void deleteEntity(EntityId id) {
        ladybugDbRepository.deleteById(id.value());
    }

    @Override
    public void deleteRelation(Relation relation) {
        ladybugDbRepository.findById(relation.from().value()).ifPresent(from -> {
            List<LadybugRelation> matching = ladybugDbRepository.findRelationsBySource(from).stream()
                    .filter(r -> r.getTargetEntity().getName().equals(relation.to().value())
                            && r.getRelationType().equals(relation.relationType().value()))
                    .collect(Collectors.toList());

            for (LadybugRelation r : matching) {
                ladybugDbRepository.deleteRelation(r);
            }
        });
    }

    @Override
    public Optional<Entity> findEntityById(EntityId id) {
        return ladybugDbRepository.findById(id.value()).map(this::toDomainEntity);
    }

    @Override
    public List<Entity> findEntitiesByType(EntityType type) {
        return ladybugDbRepository.findByType(type.value()).stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Relation> findRelationsForEntity(EntityId entityId) {
        return ladybugDbRepository.findRelationsForEntity(entityId.value()).stream()
                .map(this::mapProjectionToRelation)
                .collect(Collectors.toList());
    }

    @Override
    public List<Relation> findRelationsByType(RelationType type) {
        return ladybugDbRepository.findRelationsByRelationType(type.value()).stream()
                .map(this::mapProjectionToRelation)
                .collect(Collectors.toList());
    }

    @Override
    public List<Entity> findRelatedEntities(EntityId entityId) {
        return ladybugDbRepository.findRelatedEntities(entityId.value()).stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Set<EntityType> findAllEntityTypes() {
        return ladybugDbRepository.findAllEntityTypes().stream()
                .filter(type -> type != null && !type.isBlank())
                .map(EntityType::new)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RelationType> findAllRelationTypes() {
        return ladybugDbRepository.findAllRelationTypes().stream()
                .filter(type -> type != null && !type.isBlank())
                .map(RelationType::new)
                .collect(Collectors.toSet());
    }

    private Entity toDomainEntity(LadybugEntity ladybugEntity) {
        return new Entity(ladybugEntity.getName(), ladybugEntity.getType(), ladybugEntity.getObservations());
    }

    private Relation mapProjectionToRelation(RelationProjection projection) {
        return new Relation(projection.fromName(), projection.toName(),
                projection.relationType());
    }

    private Relation mapLadybugRelationToDomain(LadybugRelation ladybugRelation) {
        return new Relation(
                ladybugRelation.getSourceEntity().getName(),
                ladybugRelation.getTargetEntity().getName(),
                ladybugRelation.getRelationType());
    }
}
