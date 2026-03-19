package com.thecookiezen.archiledger.domain.repository;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.NoteLink;
import com.thecookiezen.archiledger.domain.model.SimilarityResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MemoryNoteRepository {
    MemoryNote save(MemoryNote note);

    Optional<MemoryNote> findById(MemoryNoteId id);

    List<MemoryNote> findAll();

    void delete(MemoryNoteId id);

    void addLink(MemoryNoteId from, MemoryNoteId to, String relationType);

    void removeLink(MemoryNoteId from, MemoryNoteId to, String relationType);

    List<NoteLink> findLinksFrom(MemoryNoteId id);

    List<MemoryNote> findByTag(String tag);

    List<MemoryNote> findLinkedNotes(MemoryNoteId noteId);

    List<MemoryNote> findLinkedNotes(MemoryNoteId noteId, String relationType, int limit);

    List<MemoryNote> findNotesUpward(MemoryNoteId noteId, int maxHops, int limit);

    Set<String> findAllTags();

    Map<String, Object> getGraph();

    void incrementRetrievalCount(MemoryNoteId id);

    List<SimilarityResult<MemoryNote>> findSimilar(float[] queryEmbedding, int topK);
}
