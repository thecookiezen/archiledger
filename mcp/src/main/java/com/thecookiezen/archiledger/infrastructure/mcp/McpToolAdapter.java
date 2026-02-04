package com.thecookiezen.archiledger.infrastructure.mcp;

import com.thecookiezen.archiledger.application.service.KnowledgeGraphService;
import com.thecookiezen.archiledger.domain.model.Entity;
import com.thecookiezen.archiledger.domain.model.EntityId;
import com.thecookiezen.archiledger.domain.model.EntityType;
import com.thecookiezen.archiledger.domain.model.Relation;
import com.thecookiezen.archiledger.domain.model.RelationType;
import com.thecookiezen.archiledger.infrastructure.mcp.dto.EntityDto;
import com.thecookiezen.archiledger.infrastructure.mcp.dto.RelationDto;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class McpToolAdapter {

        private final KnowledgeGraphService knowledgeGraphService;

        public McpToolAdapter(KnowledgeGraphService knowledgeGraphService) {
                this.knowledgeGraphService = knowledgeGraphService;
        }

        @Tool(name = "create_entities", description = "Create new entities in the knowledge graph. Entities are nodes in the graph and represent things like people, places, concepts, etc.")
        public List<EntityDto> createEntities(
                        @ToolParam(description = "List of entities to create") List<EntityDto> newEntities) {
                List<Entity> domainEntities = newEntities.stream()
                                .map(EntityDto::toDomain)
                                .collect(Collectors.toList());
                return knowledgeGraphService.createEntities(domainEntities).stream()
                                .map(EntityDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "create_relations", description = "Create new relations between entities in the knowledge graph. Relations are edges in the graph and represent how entities are connected.")
        public List<RelationDto> createRelations(
                        @ToolParam(description = "List of relations to create") List<RelationDto> newRelations) {
                List<Relation> domainRelations = newRelations.stream()
                                .map(RelationDto::toDomain)
                                .collect(Collectors.toList());
                return knowledgeGraphService.createRelations(domainRelations).stream()
                                .map(RelationDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "read_graph", description = "Read the entire knowledge graph. Returns all entities and relations.")
        public Map<String, Object> readGraph() {
                return knowledgeGraphService.readGraph();
        }

        @Tool(name = "delete_entities", description = "Delete entities from the knowledge graph by their names.")
        public void deleteEntities(@ToolParam(description = "List of entity names to delete") List<String> names) {
                List<EntityId> ids = names.stream()
                                .map(EntityId::new)
                                .collect(Collectors.toList());
                knowledgeGraphService.deleteEntities(ids);
        }

        @Tool(name = "delete_relations", description = "Delete relations from the knowledge graph.")
        public void deleteRelations(
                        @ToolParam(description = "List of relations to delete") List<RelationDto> relationsToDelete) {
                List<Relation> domainRelations = relationsToDelete.stream()
                                .map(RelationDto::toDomain)
                                .collect(Collectors.toList());
                knowledgeGraphService.deleteRelations(domainRelations);
        }

        @Tool(name = "get_entity", description = "Get a specific entity by its name from the knowledge graph. Returns the entity with its type and observations.")
        public Optional<EntityDto> getEntity(@ToolParam(description = "Name of the entity to retrieve") String name) {
                return knowledgeGraphService.getEntity(new EntityId(name))
                                .map(EntityDto::fromDomain);
        }

        @Tool(name = "get_entities_by_type", description = "Get all entities of a specific type from the knowledge graph. Example types: Person, Component, Service, Database, API.")
        public List<EntityDto> getEntitiesByType(
                        @ToolParam(description = "The entity type to filter by (e.g., 'Person', 'Component', 'Service')") String entityType) {
                return knowledgeGraphService.getEntitiesByType(new EntityType(entityType)).stream()
                                .map(EntityDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "get_relations_for_entity", description = "Get all relations (both incoming and outgoing) for a specific entity. Returns all connections the entity has with other entities.")
        public List<RelationDto> getRelationsForEntity(
                        @ToolParam(description = "Name of the entity to get relations for") String entityName) {
                return knowledgeGraphService.getRelationsForEntity(new EntityId(entityName)).stream()
                                .map(RelationDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "get_relations_by_type", description = "Get all relations of a specific type from the knowledge graph. Example types: DEPENDS_ON, USES, CONTAINS, CALLS, OWNS.")
        public List<RelationDto> getRelationsByType(
                        @ToolParam(description = "The relation type to filter by (e.g., 'DEPENDS_ON', 'USES', 'CONTAINS')") String relationType) {
                return knowledgeGraphService.getRelationsByType(new RelationType(relationType)).stream()
                                .map(RelationDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "get_related_entities", description = "Find all entities that are directly connected to a given entity (either as source or target of any relation).")
        public List<EntityDto> getRelatedEntities(
                        @ToolParam(description = "Name of the entity to find related entities for") String entityName) {
                return knowledgeGraphService.getRelatedEntities(new EntityId(entityName)).stream()
                                .map(EntityDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "get_entity_types", description = "List all unique entity types currently in the knowledge graph. Useful for discovering what types of entities exist.")
        public List<String> getEntityTypes() {
                return knowledgeGraphService.getEntityTypes().stream()
                                .map(EntityType::value)
                                .collect(Collectors.toList());
        }

        @Tool(name = "get_relation_types", description = "List all unique relation types currently in the knowledge graph. Useful for discovering what types of relationships exist.")
        public List<String> getRelationTypes() {
                return knowledgeGraphService.getRelationTypes().stream()
                                .map(RelationType::value)
                                .collect(Collectors.toList());
        }

        @Tool(name = "similarity_search", description = "Find entities most similar to a given query based on embeddings.")
        public List<String> similaritySearch(
                        @ToolParam(description = "Query to find similar entities for") String query) {
                return knowledgeGraphService.similaritySearch(query);
        }
}