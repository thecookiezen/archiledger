package com.thecookiezen.archiledger.application.service;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.SimilarityResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MemoryNoteService {
    MemoryNote createNote(MemoryNote note);

    List<MemoryNote> createNotes(List<MemoryNote> notes);

    Optional<MemoryNote> getNote(MemoryNoteId id);

    List<MemoryNote> getAllNotes();

    void deleteNote(MemoryNoteId id);

    void deleteNotes(List<MemoryNoteId> ids);

    void addLink(MemoryNoteId from, MemoryNoteId to, String relationType);

    void removeLink(MemoryNoteId from, MemoryNoteId to, String relationType);

    List<MemoryNote> getNotesByTag(String tag);

    List<MemoryNote> getLinkedNotes(MemoryNoteId noteId);

    List<MemoryNote> getLinkedNotes(MemoryNoteId noteId, String relationType, int limit);

    List<MemoryNote> getNotesUpward(MemoryNoteId noteId, int maxHops, int limit);

    Set<String> getAllTags();

    Map<String, Object> readGraph();

    List<SimilarityResult<MemoryNote>> similaritySearch(String query);
}
