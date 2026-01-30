package com.example.memory.infrastructure.mcp;

import com.example.memory.application.service.KnowledgeGraphService;
import com.example.memory.domain.model.Entity;
import com.example.memory.domain.model.Relation;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class McpToolAdapter {

    private final KnowledgeGraphService knowledgeGraphService;

    public McpToolAdapter(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Tool(name = "create_entities", description = "Create new entities in the knowledge graph. Entities are nodes in the graph and represent things like people, places, concepts, etc.")
    public List<Entity> createEntities(@ToolParam(description = "List of entities to create") List<Entity> newEntities) {
        return knowledgeGraphService.createEntities(newEntities);
    }

    @Tool(name = "create_relations", description = "Create new relations between entities in the knowledge graph. Relations are edges in the graph and represent how entities are connected.")
    public List<Relation> createRelations(@ToolParam(description = "List of relations to create") List<Relation> newRelations) {
        return knowledgeGraphService.createRelations(newRelations);
    }

    @Tool(name = "read_graph", description = "Read the entire knowledge graph. Returns all entities and relations.")
    public Map<String, Object> readGraph() {
        return knowledgeGraphService.readGraph();
    }

    @Tool(name = "search_nodes", description = "Search for nodes (entities) in the knowledge graph based on a query string.")
    public List<Entity> searchNodes(@ToolParam(description = "Query string to search for entities by name or type") String query) {
        return knowledgeGraphService.searchNodes(query);
    }

    @Tool(name = "delete_entities", description = "Delete entities from the knowledge graph by their names.")
    public void deleteEntities(@ToolParam(description = "List of entity names to delete") List<String> names) {
        knowledgeGraphService.deleteEntities(names);
    }
    
    @Tool(name = "delete_relations", description = "Delete relations from the knowledge graph.")
    public void deleteRelations(@ToolParam(description = "List of relations to delete") List<Relation> relationsToDelete) {
        knowledgeGraphService.deleteRelations(relationsToDelete);
    }
}
