package com.thecookiezen.archiledger.agenticmemory.rag;

import com.embabel.agent.rag.model.ContentElement;
import com.embabel.agent.rag.model.Retrievable;
import com.embabel.agent.rag.service.ResultExpander;
import com.embabel.agent.rag.service.VectorSearch;
import com.embabel.common.core.types.SimilarityResult;
import com.embabel.common.core.types.TextSimilaritySearchRequest;
import com.thecookiezen.archiledger.application.service.MemoryNoteService;
import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MemoryNoteSearchOperations implements VectorSearch, ResultExpander {

    private static final Logger logger = LoggerFactory.getLogger(MemoryNoteSearchOperations.class);

    private final MemoryNoteService memoryNoteService;

    public MemoryNoteSearchOperations(MemoryNoteService memoryNoteService) {
        this.memoryNoteService = memoryNoteService;
    }

    @Override
    public boolean supportsType(String type) {
        return MemoryNoteRetrievable.class.getSimpleName().equals(type);
    }

    @Override
    public <T extends Retrievable> List<SimilarityResult<T>> vectorSearch(
            TextSimilaritySearchRequest request,
            Class<T> clazz) {

        if (!clazz.isAssignableFrom(MemoryNoteRetrievable.class)) {
            return List.of();
        }

        return memoryNoteService.similaritySearch(request.getQuery(), request.getTopK(), request.getSimilarityThreshold(), 0)
            .stream()
            .map(result -> {
                T retrievable = clazz.cast(new MemoryNoteRetrievable(result.item()));
                return SimilarityResult.create(retrievable, result.score());
            })
            .toList();
    }

    @Override
    public List<ContentElement> expandResult(String id, Method method, int elementsToAdd) {
        if (id == null || id.isBlank()) {
            return List.of();
        }

        MemoryNoteId noteId = new MemoryNoteId(id);
        
        return switch (method) {
            case SEQUENCE -> expandByLinks(noteId, elementsToAdd);
            case ZOOM_OUT -> expandUpward(noteId, elementsToAdd);
        };
    }

    private List<ContentElement> expandByLinks(MemoryNoteId noteId, int maxElements) {
        List<MemoryNote> linkedNotes = memoryNoteService.getNotesUpward(noteId, 1, maxElements);
        
        logger.debug("Expanding note {} found {} linked notes", noteId.value(), linkedNotes.size());
        
        return linkedNotes.stream()
            .map(MemoryNoteRetrievable::new)
            .collect(Collectors.toList());
    }

    private List<ContentElement> expandUpward(MemoryNoteId noteId, int maxElements) {
        int maxHops = 3;
        List<MemoryNote> upwardNotes = memoryNoteService.getNotesUpward(noteId, maxHops, maxElements);
        
        logger.debug("Expanding note {} upward found {} notes", noteId.value(), upwardNotes.size());
        
        return upwardNotes.stream()
            .map(MemoryNoteRetrievable::new)
            .collect(Collectors.toList());
    }
}
