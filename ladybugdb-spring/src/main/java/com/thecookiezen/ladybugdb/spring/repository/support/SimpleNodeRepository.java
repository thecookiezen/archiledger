package com.thecookiezen.ladybugdb.spring.repository.support;

import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.mapper.ValueMappers;
import com.thecookiezen.ladybugdb.spring.repository.NodeRepository;

import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link NodeRepository}.
 * Provides basic CRUD operations for node entities using
 * {@link LadybugDBTemplate}.
 *
 * @param <T>  the node entity type
 * @param <ID> the primary key type
 */
public class SimpleNodeRepository<T, R, ID> implements NodeRepository<T, ID, R, T> {

        private static final Logger logger = LoggerFactory.getLogger(SimpleNodeRepository.class);

        @SuppressWarnings("unused")
        private final Class<T> domainType;
        protected final LadybugDBTemplate template;
        protected final NodeMetadata<T> metadata;
        protected final RelationshipMetadata<R> relationshipMetadata;
        protected final EntityDescriptor<T> descriptor;
        protected final EntityDescriptor<R> relationshipDescriptor;

        public SimpleNodeRepository(LadybugDBTemplate template, Class<T> domainType, Class<R> relationshipType,
                        EntityDescriptor<T> descriptor, EntityDescriptor<R> relationshipDescriptor) {
                this.template = template;
                this.domainType = domainType;
                this.metadata = new NodeMetadata<>(domainType);
                this.relationshipMetadata = new RelationshipMetadata<>(relationshipType);
                this.descriptor = descriptor;
                this.relationshipDescriptor = relationshipDescriptor;
                logger.debug("Created node repository for entity type: {}", domainType.getName());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <S extends T> S save(S entity) {
                logger.debug("Saving node entity: {}", entity);

                Node n = Cypher.node(metadata.getNodeLabel()).named("n")
                                .withProperties(metadata.getIdPropertyName(), Cypher.literalOf(metadata.getId(entity)));

                var decomposed = descriptor.writer().decompose(entity);

                var setOperations = decomposed.entrySet().stream()
                                .map(e -> n.property(e.getKey()).to(Cypher.literalOf(e.getValue())))
                                .toList();

                Statement statement = Cypher
                                .merge(n)
                                .set(setOperations)
                                .returning(n)
                                .build();

                return (S) template.queryForObject(statement, descriptor.reader())
                                .orElseThrow(() -> new RuntimeException("Failed to save node entity: " + entity));
        }

        @Override
        public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
                List<S> result = new ArrayList<>();
                for (S entity : entities) {
                        result.add(save(entity));
                }
                return result;
        }

        @Override
        public Optional<T> findById(ID id) {
                logger.debug("Finding node by ID: {}", id);

                Node n = Cypher.node(metadata.getNodeLabel()).named("n")
                                .withProperties(metadata.getIdPropertyName(), Cypher.literalOf(id));

                Statement statement = Cypher.match(n)
                                .returning(n)
                                .build();

                return template.queryForObject(statement, descriptor.reader());
        }

        @Override
        public boolean existsById(ID id) {
                return findById(id).isPresent();
        }

        @Override
        public Iterable<T> findAll() {
                logger.debug("Finding all nodes of type: {}", metadata.getNodeLabel());
                Node n = Cypher.node(metadata.getNodeLabel()).named("n");
                Statement statement = Cypher.match(n)
                                .returning(n)
                                .build();

                return template.query(statement, descriptor.reader());
        }

        @Override
        public Iterable<T> findAllById(Iterable<ID> ids) {
                List<T> result = new ArrayList<>();
                for (ID id : ids) {
                        findById(id).ifPresent(result::add);
                }
                return result;
        }

        @Override
        public long count() {
                logger.debug("Counting nodes of type: {}", metadata.getNodeLabel());
                Node n = Cypher.node(metadata.getNodeLabel()).named("n");
                Statement statement = Cypher.match(n)
                                .returning(Cypher.count(n).as("count"))
                                .build();

                return template.queryForObject(statement, (row) -> (Long) ValueMappers.asLong(row.getValue("count")))
                                .orElseThrow(() -> new RuntimeException(
                                                "Failed to count nodes of type: " + metadata.getNodeLabel()));
        }

        @Override
        public void deleteById(ID id) {
                logger.debug("Deleting node by ID: {}", id);

                Node n = Cypher.node(metadata.getNodeLabel()).named("n")
                                .withProperties(metadata.getIdPropertyName(), Cypher.literalOf(id));

                Statement statement = Cypher.match(n)
                                .detachDelete(n)
                                .build();

                template.execute(statement);
        }

        @Override
        public void delete(T entity) {
                ID id = metadata.getId(entity);
                deleteById(id);
        }

        @Override
        public void deleteAllById(Iterable<? extends ID> ids) {
                for (ID id : ids) {
                        deleteById(id);
                }
        }

        @Override
        public void deleteAll(Iterable<? extends T> entities) {
                for (T entity : entities) {
                        delete(entity);
                }
        }

        @Override
        public void deleteAll() {
                logger.debug("Deleting all nodes of type: {}", metadata.getNodeLabel());
                Node n = Cypher.node(metadata.getNodeLabel()).named("n");
                Statement statement = Cypher.match(n)
                                .detachDelete(n)
                                .build();

                template.execute(statement);
        }

        @Override
        public Optional<R> findRelationById(ID id) {
                logger.debug("Finding relationship by ID: {}", id);

                Node s = Cypher.node(metadata.getNodeLabel()).named("s");

                Node t = Cypher.node(metadata.getNodeLabel()).named("t");

                var rel = s.relationshipTo(t, relationshipMetadata.getRelationshipTypeName()).named("rel")
                                .withProperties(relationshipMetadata.getIdPropertyName(), Cypher.literalOf(id));

                Statement statement = Cypher.match(rel)
                                .returning(s, t, rel)
                                .build();

                return template.queryForObject(statement, relationshipDescriptor.reader());
        }

        @Override
        public R createRelation(T source, T target, R relationship) {
                logger.debug("Creating relationship: {} -> {}", source, target);

                Node s = Cypher.node(metadata.getNodeLabel()).named("s")
                                .withProperties(metadata.getIdPropertyName(), Cypher.literalOf(metadata.getId(source)));

                Node t = Cypher.node(metadata.getNodeLabel()).named("t")
                                .withProperties(metadata.getIdPropertyName(), Cypher.literalOf(metadata.getId(target)));

                var rel = s.relationshipTo(t, relationshipMetadata.getRelationshipTypeName()).named("rel");

                var setOperations = relationshipDescriptor.writer().decompose(relationship).entrySet().stream()
                                .filter(e -> !e.getKey().equals(relationshipMetadata.getSourceFieldName())
                                                && !e.getKey().equals(relationshipMetadata.getTargetFieldName()))
                                .map(e -> rel.property(e.getKey()).to(Cypher.literalOf(e.getValue())))
                                .toList();

                var matchMerge = Cypher.match(s, t).merge(rel);

                Statement statement;
                if (!setOperations.isEmpty()) {
                        statement = matchMerge.set(setOperations).returning(s, t, rel).build();
                } else {
                        statement = matchMerge.returning(s, t, rel).build();
                }

                return template.queryForObject(statement, relationshipDescriptor.reader())
                                .orElseThrow(() -> new RuntimeException(
                                                "Failed to create relationship: " + relationship));
        }

        @Override
        public List<R> findRelationsBySource(T source) {
                logger.debug("Finding relationships by source: {}", source);

                Node s = Cypher.node(metadata.getNodeLabel()).named("s")
                                .withProperties(metadata.getIdPropertyName(), Cypher.literalOf(metadata.getId(source)));

                Node t = Cypher.node(metadata.getNodeLabel()).named("t");

                var rel = s.relationshipTo(t, relationshipMetadata.getRelationshipTypeName()).named("rel");

                Statement statement = Cypher.match(rel)
                                .returning(s, t, rel)
                                .build();

                return template.query(statement, relationshipDescriptor.reader());
        }

        @Override
        public List<R> findAllRelations() {
                logger.debug("Finding all relationships of type: {}", relationshipMetadata.getRelationshipTypeName());

                Node s = Cypher.node(metadata.getNodeLabel()).named("s");

                Node t = Cypher.node(metadata.getNodeLabel()).named("t");

                var rel = s.relationshipTo(t, relationshipMetadata.getRelationshipTypeName()).named("rel");

                Statement statement = Cypher.match(rel)
                                .returning(s, t, rel)
                                .build();

                return template.query(statement, relationshipDescriptor.reader());
        }

        @Override
        public void deleteRelation(R relationship) {
                logger.debug("Deleting relationship: {}", relationship);

                Node s = Cypher.node(metadata.getNodeLabel()).named("s");

                Node t = Cypher.node(metadata.getNodeLabel()).named("t");

                var rel = s.relationshipTo(t, relationshipMetadata.getRelationshipTypeName()).named("rel")
                                .withProperties(relationshipMetadata.getIdPropertyName(),
                                                Cypher.literalOf(relationshipMetadata.getId(relationship)));

                Statement statement = Cypher.match(rel)
                                .delete(rel)
                                .build();

                template.execute(statement);
        }

        @Override
        public void deleteRelationBySource(T source) {
                logger.debug("Deleting relationship by source: {}", source);

                Node s = Cypher.node(metadata.getNodeLabel()).named("s")
                                .withProperties(metadata.getIdPropertyName(), Cypher.literalOf(metadata.getId(source)));

                Node t = Cypher.node(metadata.getNodeLabel()).named("t");

                var rel = s.relationshipTo(t, relationshipMetadata.getRelationshipTypeName()).named("rel");

                Statement statement = Cypher.match(rel)
                                .delete(rel)
                                .build();

                template.execute(statement);
        }

        protected LadybugDBTemplate getTemplate() {
                return template;
        }

        protected NodeMetadata<T> getMetadata() {
                return metadata;
        }
}
