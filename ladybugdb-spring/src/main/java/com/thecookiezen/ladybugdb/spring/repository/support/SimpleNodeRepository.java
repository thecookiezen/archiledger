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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

        private final String findByIdStatement;
        private final String saveStatement;
        private final String deleteStatement;
        private final String deleteByIdStatement;
        private final String findAllStatement;
        private final String findAllByIdStatement;
        private final String countStatement;
        private final String createRelationStatement;

        public SimpleNodeRepository(LadybugDBTemplate template, Class<T> domainType, Class<R> relationshipType,
                        EntityDescriptor<T> descriptor, EntityDescriptor<R> relationshipDescriptor) {
                this.template = template;
                this.domainType = domainType;
                this.metadata = new NodeMetadata<>(domainType);
                this.relationshipMetadata = new RelationshipMetadata<>(relationshipType);
                this.descriptor = descriptor;
                this.relationshipDescriptor = relationshipDescriptor;
                logger.debug("Created node repository for entity type: {}", domainType.getName());

                Node n = Cypher.node(metadata.getNodeLabel()).named("n");
                findByIdStatement = Cypher.match(n)
                                .where(n.property(metadata.getIdPropertyName())
                                                .isEqualTo(Cypher.parameter("id")))
                                .returning(n)
                                .build()
                                .getCypher();

                Node mergeNode = Cypher.node(metadata.getNodeLabel()).named("n")
                                .withProperties(metadata.getIdPropertyName(),
                                                Cypher.parameter(metadata.getIdPropertyName()));

                var setOperations = metadata.getPropertyNames().stream()
                                .filter(e -> !e.equals(metadata.getIdPropertyName()))
                                .map(e -> mergeNode.property(e).to(Cypher.parameter(e)))
                                .toList();

                if (setOperations.isEmpty()) {
                        saveStatement = Cypher.merge(mergeNode).returning(mergeNode).build()
                                        .getCypher();
                } else {
                        saveStatement = Cypher.merge(mergeNode).set(setOperations).returning(mergeNode).build()
                                        .getCypher();
                }

                findAllStatement = Cypher.match(n).returning(n).build()
                                .getCypher();

                findAllByIdStatement = Cypher.match(n)
                                .where(n.property(metadata.getIdPropertyName()).in(Cypher.parameter("ids")))
                                .returning(n)
                                .build()
                                .getCypher();

                countStatement = Cypher.match(n)
                                .returning(Cypher.count(n).as("count"))
                                .build()
                                .getCypher();

                deleteStatement = Cypher.match(n)
                                .where(n.property(metadata.getIdPropertyName())
                                                .isEqualTo(Cypher.parameter("id")))
                                .detachDelete(n)
                                .build()
                                .getCypher();

                deleteByIdStatement = Cypher.match(n)
                                .where(n.property(metadata.getIdPropertyName())
                                                .in(Cypher.parameter("ids")))
                                .detachDelete(n)
                                .build()
                                .getCypher();

                Node s = Cypher.node(metadata.getNodeLabel()).named("s")
                                .withProperties(metadata.getIdPropertyName(), Cypher.parameter("sourceId"));

                Node t = Cypher.node(metadata.getNodeLabel()).named("t")
                                .withProperties(metadata.getIdPropertyName(), Cypher.parameter("targetId"));

                var rel = s.relationshipTo(t, relationshipMetadata.getRelationshipTypeName()).named("rel");

                var ops = relationshipMetadata.getPropertyNames().stream()
                                .filter(propertyName -> !propertyName.equals(relationshipMetadata.getSourceFieldName())
                                                && !propertyName.equals(relationshipMetadata.getTargetFieldName()))
                                .map(propertyName -> rel.property(propertyName).to(Cypher.parameter(propertyName)))
                                .toList();

                var matchMerge = Cypher.match(s, t).merge(rel);

                if (!ops.isEmpty()) {
                        createRelationStatement = matchMerge.set(ops).returning(s, t, rel).build().getCypher();
                } else {
                        createRelationStatement = matchMerge.returning(s, t, rel).build().getCypher();
                }
        }

        @SuppressWarnings("unchecked")
        @Override
        public <S extends T> S save(S entity) {
                logger.debug("Saving node entity: {}", entity);

                Map<String, Object> parameters = new HashMap<>(descriptor.writer().decompose(entity));
                parameters.put(metadata.getIdPropertyName(), metadata.getId(entity));

                return (S) template
                                .queryForObject(saveStatement, parameters, descriptor.reader())
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

                return template.queryForObject(findByIdStatement, Map.of("id", id), descriptor.reader());
        }

        @Override
        public boolean existsById(ID id) {
                return findById(id).isPresent();
        }

        @Override
        public Iterable<T> findAll() {
                logger.debug("Finding all nodes of type: {}", metadata.getNodeLabel());

                return template.query(findAllStatement, descriptor.reader());
        }

        @Override
        public Iterable<T> findAllById(Iterable<ID> ids) {
                logger.debug("Finding all nodes of type {} by IDs", metadata.getNodeLabel());
                List<ID> idList = new ArrayList<>();
                ids.forEach(idList::add);

                if (idList.isEmpty()) {
                        return Collections.emptyList();
                }

                return template.query(findAllByIdStatement, Map.of("ids", idList), descriptor.reader());
        }

        @Override
        public long count() {
                logger.debug("Counting nodes of type: {}", metadata.getNodeLabel());

                return template.queryForObject(countStatement,
                                (row) -> (Long) ValueMappers.asLong(row.getValue("count")))
                                .orElseThrow(() -> new RuntimeException(
                                                "Failed to count nodes of type: " + metadata.getNodeLabel()));
        }

        @Override
        public void deleteById(ID id) {
                logger.debug("Deleting node by ID: {}", id);

                template.execute(deleteStatement, Map.of("id", id));
        }

        @Override
        public void delete(T entity) {
                ID id = metadata.getId(entity);
                deleteById(id);
        }

        @Override
        public void deleteAllById(Iterable<? extends ID> ids) {
                logger.debug("Deleting all nodes of type {} by IDs", metadata.getNodeLabel());
                List<ID> idList = new ArrayList<>();
                ids.forEach(idList::add);

                if (idList.isEmpty()) {
                        return;
                }

                template.execute(deleteByIdStatement, Map.of("ids", idList));
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

                var params = relationshipDescriptor.writer().decompose(relationship).entrySet().stream()
                                .filter(e -> !e.getKey().equals(relationshipMetadata.getSourceFieldName())
                                                && !e.getKey().equals(relationshipMetadata.getTargetFieldName()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                params.put("sourceId", metadata.getId(source));
                params.put("targetId", metadata.getId(target));

                return template.queryForObject(createRelationStatement, params, relationshipDescriptor.reader())
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
