package com.thecookiezen.archiledger.agenticmemory.rag;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.Verbosity;
import com.embabel.agent.rag.service.ResultExpander.Method;
import com.embabel.common.core.types.TextSimilaritySearchRequest;
import com.thecookiezen.archiledger.agenticmemory.domain.UpsertMemoryRequest;
import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.SimilarityResult;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgenticMemoryMcpTools {

    private final MemoryNoteSearchOperations searchOperations;
    private final AgentPlatform agentPlatform;

    public AgenticMemoryMcpTools(@Qualifier("archiledgerSearchOperations") MemoryNoteSearchOperations searchOperations,
        AgentPlatform agentPlatform) {
        this.searchOperations = searchOperations;
        this.agentPlatform = agentPlatform;
    }

    @Tool(name = "memory_vector_search", description = "Perform semantic similarity search across memory notes. Returns the most relevant notes based on vector embeddings of their content.")
    public List<SimilarityResult<MemoryNote>> vectorSearch(
            @ToolParam(description = "Natural language query to search for") String query,
            @ToolParam(description = "Maximum number of results to return", required = false) Integer topK,
            @ToolParam(description = "Minimum similarity threshold (0.0 to 1.0)", required = false) Double threshold) {
        
        int limit = topK != null ? topK : 10;
        double simThreshold = threshold != null ? threshold : 0.5;
        
        return searchOperations.vectorSearch(TextSimilaritySearchRequest.create(query, simThreshold, limit), MemoryNoteRetrievable.class)
            .stream()
            .map(m -> new SimilarityResult<MemoryNote>(m.getMatch().note(), m.getScore()))
            .toList();
    }

    @Tool(name = "memory_broaden_search", description = "Given a note ID, expand to find connected/linked notes. Useful for exploring related memories.")
    public List<MemoryNote> broadenChunk(
            @ToolParam(description = "ID of the note to expand from") String noteId,
            @ToolParam(description = "Maximum number of connected notes to return", required = false) Integer limit) {
        
        int maxElements = limit != null ? limit : 10;
        
        return searchOperations.expandResult(noteId, Method.SEQUENCE, maxElements)
            .stream()
            .map(m -> ((MemoryNoteRetrievable) m).note())
            .toList();
    }

    @Tool(name = "memory_zoom_out", description = "Given a note ID, traverse upward in the knowledge graph to find parent/related notes within a few hops.")
    public List<MemoryNote> zoomOut(
            @ToolParam(description = "ID of the note to zoom out from") String noteId,
            @ToolParam(description = "Maximum number of notes to return", required = false) Integer limit) {
        
        int maxElements = limit != null ? limit : 10;
        
        return searchOperations.expandResult(noteId, Method.ZOOM_OUT, maxElements)
            .stream()
            .map(m -> ((MemoryNoteRetrievable) m).note())
            .toList();
    }

    @Tool(name = "agentic_memory_write", description = "Store content in agentic memory as a memory note. Creates a basic note with the provided content and optional tags.")
    public MemoryNote writeMemory(@ToolParam(description = "The content to store in memory") String content) {
         var invocation = AgentInvocation
            .builder(agentPlatform)
            .options(new ProcessOptions()
                .withVerbosity(new Verbosity()
                    .withShowPrompts(true)
                    .withShowLlmResponses(true)
                    .withDebug(true)))
            .build(MemoryNote.class);

        return invocation.invoke(new UpsertMemoryRequest(content));
    }
}
