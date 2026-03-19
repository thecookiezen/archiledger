package com.thecookiezen.archiledger.application.service;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.SimilarityResult;
import com.thecookiezen.archiledger.domain.repository.EmbeddingsService;
import com.thecookiezen.archiledger.domain.repository.MemoryNoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryNoteServiceImplTest {

    @Mock
    private MemoryNoteRepository repository;

    @Mock
    private EmbeddingsService embeddingsService;

    @InjectMocks
    private MemoryNoteServiceImpl service;

    private MemoryNote sampleNote(String id) {
        return new MemoryNote(
                new MemoryNoteId(id),
                "Sample content for " + id,
                List.of("keyword1"),
                "test-context",
                List.of("tag1"),
                List.of(),
                "2026-03-04T16:00:00Z",
                0,
                null);
    }

    @Test
    void createNote_savesAndGeneratesEmbeddings() {
        MemoryNote note = sampleNote("note-1");
        float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
        MemoryNote noteWithEmbedding = note.withEmbedding(embedding);
        when(embeddingsService.generateEmbeddings(note)).thenReturn(embedding);
        when(repository.save(noteWithEmbedding)).thenReturn(noteWithEmbedding);

        MemoryNote result = service.createNote(note);

        assertEquals("note-1", result.id().value());
        verify(embeddingsService).generateEmbeddings(note);
        verify(repository).save(noteWithEmbedding);
    }

    @Test
    void createNotes_savesMultiple() {
        MemoryNote note1 = sampleNote("note-1");
        MemoryNote note2 = sampleNote("note-2");
        when(embeddingsService.generateEmbeddings(note1)).thenReturn(new float[] { 0.1f });
        when(embeddingsService.generateEmbeddings(note2)).thenReturn(new float[] { 0.2f });
        when(repository.save(any(MemoryNote.class))).thenAnswer(inv -> inv.getArgument(0));

        List<MemoryNote> result = service.createNotes(List.of(note1, note2));

        assertEquals(2, result.size());
        verify(repository, times(2)).save(any(MemoryNote.class));
    }

    @Test
    void getNote_incrementsRetrievalCount() {
        MemoryNote note = sampleNote("note-1");
        when(repository.findById(new MemoryNoteId("note-1"))).thenReturn(Optional.of(note));

        Optional<MemoryNote> result = service.getNote(new MemoryNoteId("note-1"));

        assertTrue(result.isPresent());
        verify(repository).incrementRetrievalCount(new MemoryNoteId("note-1"));
    }

    @Test
    void getNote_whenNotFound_doesNotIncrementRetrievalCount() {
        when(repository.findById(new MemoryNoteId("missing"))).thenReturn(Optional.empty());

        Optional<MemoryNote> result = service.getNote(new MemoryNoteId("missing"));

        assertTrue(result.isEmpty());
        verify(repository, never()).incrementRetrievalCount(any());
    }

    @Test
    void addLink_delegatesToRepository() {
        MemoryNoteId from = new MemoryNoteId("A");
        MemoryNoteId to = new MemoryNoteId("B");

        service.addLink(from, to, "DEPENDS_ON");

        verify(repository).addLink(from, to, "DEPENDS_ON");
    }

    @Test
    void removeLink_delegatesToRepository() {
        MemoryNoteId from = new MemoryNoteId("A");
        MemoryNoteId to = new MemoryNoteId("B");

        service.removeLink(from, to, "DEPENDS_ON");

        verify(repository).removeLink(from, to, "DEPENDS_ON");
    }

    @Test
    void getNotesByTag_delegatesToRepository() {
        List<MemoryNote> notes = List.of(sampleNote("note-1"), sampleNote("note-2"));
        when(repository.findByTag("architecture")).thenReturn(notes);

        List<MemoryNote> result = service.getNotesByTag("architecture");

        assertEquals(2, result.size());
        verify(repository).findByTag("architecture");
    }

    @Test
    void getLinkedNotes_delegatesToRepository() {
        MemoryNoteId noteId = new MemoryNoteId("A");
        List<MemoryNote> linked = List.of(sampleNote("B"), sampleNote("C"));
        when(repository.findLinkedNotes(noteId)).thenReturn(linked);

        List<MemoryNote> result = service.getLinkedNotes(noteId);

        assertEquals(2, result.size());
        verify(repository).findLinkedNotes(noteId);
    }

    @Test
    void getLinkedNotes_withRelationTypeAndLimit_fetchesNotes() {
        MemoryNoteId noteId = new MemoryNoteId("A");
        MemoryNote noteB = sampleNote("B");
        MemoryNote noteC = sampleNote("C");
        when(repository.findLinkedNotes(noteId, "CONTAINS", 5)).thenReturn(List.of(noteB, noteC));

        List<MemoryNote> result = service.getLinkedNotes(noteId, "CONTAINS", 5);

        assertEquals(2, result.size());
        verify(repository).findLinkedNotes(noteId, "CONTAINS", 5);
    }

    @Test
    void getNotesUpward_fetchesNotes() {
        MemoryNoteId noteId = new MemoryNoteId("A");
        MemoryNote parent = sampleNote("PARENT");
        when(repository.findNotesUpward(noteId, 3, 5)).thenReturn(List.of(parent));

        List<MemoryNote> result = service.getNotesUpward(noteId, 3, 5);

        assertEquals(1, result.size());
        assertEquals("PARENT", result.get(0).id().value());
        verify(repository).findNotesUpward(noteId, 3, 5);
    }

    @Test
    void getAllTags_delegatesToRepository() {
        when(repository.findAllTags()).thenReturn(Set.of("architecture", "decision"));

        Set<String> result = service.getAllTags();

        assertEquals(2, result.size());
        verify(repository).findAllTags();
    }

    @Test
    void readGraph_delegatesToRepository() {
        when(repository.getGraph()).thenReturn(Map.of("notes", List.of(), "links", List.of()));

        Map<String, Object> graph = service.readGraph();

        assertNotNull(graph);
        verify(repository).getGraph();
    }

    @Test
    void similaritySearch_embedsQueryAndDelegatesToRepository() {
        float[] queryEmbedding = new float[] { 0.1f, 0.2f, 0.3f };
        MemoryNote matchedNote = sampleNote("match-1");
        when(embeddingsService.embed("architecture")).thenReturn(queryEmbedding);
        when(repository.findSimilar(queryEmbedding, 10))
                .thenReturn(List.of(new SimilarityResult<>(matchedNote, 0.95)));

        List<SimilarityResult<MemoryNote>> results = service.similaritySearch("architecture");

        assertEquals(1, results.size());
        assertEquals("Sample content for match-1", results.get(0).item().content());
        verify(embeddingsService).embed("architecture");
        verify(repository).findSimilar(queryEmbedding, 10);
    }
}
