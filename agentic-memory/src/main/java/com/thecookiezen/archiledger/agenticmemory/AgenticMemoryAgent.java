package com.thecookiezen.archiledger.agenticmemory;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.rag.tools.ToolishRag;
import com.thecookiezen.archiledger.agenticmemory.domain.EvolutionDecision;
import com.thecookiezen.archiledger.agenticmemory.domain.NeighborUpdate;
import com.thecookiezen.archiledger.agenticmemory.domain.NoteAnalysis;
import com.thecookiezen.archiledger.agenticmemory.domain.UpsertMemoryRequest;
import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.NoteLink;
import com.thecookiezen.archiledger.application.service.MemoryNoteService;
import com.thecookiezen.archiledger.agenticmemory.rag.MemoryNoteSearchOperations;
import com.thecookiezen.archiledger.agenticmemory.rag.MemoryNoteRetrievable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Agent(description = "Manages memory storage with automatic content analysis, link generation, and memory evolution")
public class AgenticMemoryAgent {

    private static final Logger logger = LoggerFactory.getLogger(AgenticMemoryAgent.class);

    private final AgenticMemoryProperties properties;
    private final MemoryNoteService memoryNoteService;
    private final ToolishRag memoryRag;

    public AgenticMemoryAgent(AgenticMemoryProperties properties, MemoryNoteService memoryNoteService, MemoryNoteSearchOperations memoryNoteSearchOperations) {
        this.properties = properties;
        this.memoryNoteService = memoryNoteService;
        this.memoryRag = new ToolishRag("memory-notes", "Historical memories for finding related content and establishing connections",
                memoryNoteSearchOperations)
            .withSearchFor(List.of(MemoryNoteRetrievable.class));
    }

    @Action
    MemoryNote analyzeContent(UpsertMemoryRequest request, Ai ai) {
        NoteAnalysis analysis = ai.withLlm(properties.chatLlm())
            .rendering("agenticmemory/analyze_content")
            .createObject(NoteAnalysis.class, Map.of("content", request.content()));

        logger.info("Analysis complete: keywords={}, context={}, tags={}", 
            analysis.keywords(), analysis.context(), analysis.tags());

        var noteId = new MemoryNoteId(UUID.randomUUID().toString());
        return new MemoryNote(
            noteId,
            request.content(),
            analysis.keywords(),
            analysis.context(),
            analysis.tags(),
            List.of(),
            Instant.now().toString(),
            0,
            null
        );
    }

    @Action
    EvolutionDecision evaluateEvolution(MemoryNote newNote, Ai ai) {
        EvolutionDecision evolutionDecision = ai.withLlm(properties.chatLlm())
            .withReference(memoryRag)
            .rendering("agenticmemory/evaluate_evolution")
            .createObject(EvolutionDecision.class, Map.of("newNote", newNote));

            logger.info("Found {} similar neighbors", evolutionDecision.neighborUpdates().size());
            return evolutionDecision;
    }

    @Action
    @AchievesGoal(description = "Store the request in agentic memory as memory note")
    public MemoryNote storeMemory(EvolutionDecision evolutionDecision, MemoryNote newNote, Ai ai) {
        var content = newNote.content();
        logger.info("Storing memory: {}...", content.substring(0, Math.min(50, content.length())));
        
        if (evolutionDecision.shouldEvolve() && !evolutionDecision.neighborUpdates().isEmpty()) {
            newNote = applyEvolution(newNote, evolutionDecision);
            updateNeighbors(evolutionDecision.neighborUpdates());
        }

        logger.info("saving memory {}", newNote);
        var savedNote = memoryNoteService.createNote(newNote);
        logger.info("Memory stored with id: {}", savedNote.id().value());
        return savedNote;
    }

    private MemoryNote applyEvolution(MemoryNote note, EvolutionDecision decision) {
        var links = decision.suggestedLinks().stream()
            .map(sl -> new NoteLink(sl.targetId(), sl.relationType(), sl.context()))
            .toList();

        logger.info("Applying evolution: {} links, {} updated tags", 
            links.size(), decision.updatedTags().size());

        return new MemoryNote(
            note.id(),
            note.content(),
            note.keywords(),
            note.context(),
            decision.updatedTags().isEmpty() ? note.tags() : decision.updatedTags(),
            links,
            note.timestamp(),
            note.retrievalCount(),
            note.embedding()
        );
    }

    private void updateNeighbors(List<NeighborUpdate> updates) {
        for (var update : updates) {
            memoryNoteService.getNote(new MemoryNoteId(update.noteId())).ifPresent(neighbor -> {
                var updatedNeighbor = new MemoryNote(
                    neighbor.id(),
                    neighbor.content(),
                    neighbor.keywords(),
                    update.newContext().isBlank() ? neighbor.context() : update.newContext(),
                    update.newTags().isEmpty() ? neighbor.tags() : update.newTags(),
                    neighbor.links(),
                    neighbor.timestamp(),
                    neighbor.retrievalCount(),
                    neighbor.embedding()
                );
                memoryNoteService.createNote(updatedNeighbor);
                logger.info("Updated neighbor: {}", update.noteId());
            });
        }
    }
}
