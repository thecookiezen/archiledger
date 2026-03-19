package com.thecookiezen.archiledger.infrastructure.persistence;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.NoteLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMemoryNoteRepositoryTest {

    private InMemoryMemoryNoteRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMemoryNoteRepository();
    }

    private MemoryNote sampleNote(String id, List<String> tags) {
        return new MemoryNote(
                new MemoryNoteId(id),
                "Content for " + id,
                List.of("keyword1"),
                "test-context",
                tags,
                List.of(),
                "2026-03-04T16:00:00Z",
                0,
                null);
    }

    @Test
    void saveAndFindNote() {
        MemoryNote note = sampleNote("note-1", List.of("architecture"));
        repository.save(note);

        List<MemoryNote> notes = repository.findAll();
        assertEquals(1, notes.size());
        assertEquals("note-1", notes.get(0).id().value());
    }

    @Test
    void findById_whenExists_returnsNote() {
        repository.save(sampleNote("note-1", List.of("architecture")));

        Optional<MemoryNote> result = repository.findById(new MemoryNoteId("note-1"));

        assertTrue(result.isPresent());
        assertEquals("note-1", result.get().id().value());
    }

    @Test
    void findById_whenNotExists_returnsEmpty() {
        Optional<MemoryNote> result = repository.findById(new MemoryNoteId("nonexistent"));
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteNote() {
        repository.save(sampleNote("note-1", List.of("tag")));
        repository.addLink(new MemoryNoteId("note-1"), new MemoryNoteId("note-1"), "SELF_REF");

        repository.delete(new MemoryNoteId("note-1"));

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void addAndFindLink() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "DEPENDS_ON");

        List<NoteLink> links = repository.findLinksFrom(new MemoryNoteId("A"));
        assertEquals(1, links.size());
        assertEquals("B", links.get(0).target().value());
        assertEquals("DEPENDS_ON", links.get(0).relationType());
    }

    @Test
    void addLink_shouldNotCreateDuplicate() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "DEPENDS_ON");
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "DEPENDS_ON");

        List<NoteLink> links = repository.findLinksFrom(new MemoryNoteId("A"));
        assertEquals(1, links.size());
    }

    @Test
    void removeLink() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "DEPENDS_ON");

        repository.removeLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "DEPENDS_ON");

        assertTrue(repository.findLinksFrom(new MemoryNoteId("A")).isEmpty());
    }

    @Test
    void findById_hydratesLinks() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CALLS");

        Optional<MemoryNote> result = repository.findById(new MemoryNoteId("A"));

        assertTrue(result.isPresent());
        assertEquals(1, result.get().links().size());
        assertEquals("B", result.get().links().get(0).target().value());
    }

    @Test
    void findByTag_returnsOnlyMatchingTags() {
        repository.save(sampleNote("note-1", List.of("architecture", "backend")));
        repository.save(sampleNote("note-2", List.of("architecture")));
        repository.save(sampleNote("note-3", List.of("frontend")));

        List<MemoryNote> architectureNotes = repository.findByTag("architecture");
        List<MemoryNote> frontendNotes = repository.findByTag("frontend");

        assertEquals(2, architectureNotes.size());
        assertEquals(1, frontendNotes.size());
    }

    @Test
    void findLinkedNotes_returnsBothDirections() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.save(sampleNote("C", List.of()));
        repository.save(sampleNote("D", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CALLS");
        repository.addLink(new MemoryNoteId("C"), new MemoryNoteId("A"), "DEPENDS_ON");

        List<MemoryNote> linked = repository.findLinkedNotes(new MemoryNoteId("A"));

        assertEquals(2, linked.size());
        Set<String> ids = Set.of(linked.get(0).id().value(), linked.get(1).id().value());
        assertTrue(ids.contains("B"));
        assertTrue(ids.contains("C"));
        assertFalse(ids.contains("D"));
    }

    @Test
    void findLinkedNotes_withRelationTypeAndLimit_filtersCorrectly() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.save(sampleNote("C", List.of()));
        repository.save(sampleNote("D", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CONTAINS");
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("C"), "CONTAINS");
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("D"), "RELATED_TO");

        List<MemoryNote> result = repository.findLinkedNotes(new MemoryNoteId("A"), "CONTAINS", 2);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("B"))));
        assertTrue(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("C"))));
    }

    @Test
    void findLinkedNotes_withLimit_respectsLimit() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.save(sampleNote("C", List.of()));
        repository.save(sampleNote("D", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CONTAINS");
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("C"), "CONTAINS");
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("D"), "CONTAINS");

        List<MemoryNote> result = repository.findLinkedNotes(new MemoryNoteId("A"), "CONTAINS", 2);

        assertEquals(2, result.size());
    }

    @Test
    void findLinkedNotes_withRelationType_returnsEmptyWhenNoMatch() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CONTAINS");

        List<MemoryNote> result = repository.findLinkedNotes(new MemoryNoteId("A"), "RELATED_TO", 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void findNotesUpward_traversesMultipleHops() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.save(sampleNote("C", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CONTAINS");
        repository.addLink(new MemoryNoteId("B"), new MemoryNoteId("C"), "CONTAINS");

        List<MemoryNote> result = repository.findNotesUpward(new MemoryNoteId("A"), 2, 10);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("B"))));
        assertTrue(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("C"))));
    }

    @Test
    void findNotesUpward_respectsMaxHops() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.save(sampleNote("C", List.of()));
        repository.save(sampleNote("D", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CONTAINS");
        repository.addLink(new MemoryNoteId("B"), new MemoryNoteId("C"), "CONTAINS");
        repository.addLink(new MemoryNoteId("C"), new MemoryNoteId("D"), "CONTAINS");

        List<MemoryNote> result = repository.findNotesUpward(new MemoryNoteId("A"), 2, 10);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("B"))));
        assertTrue(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("C"))));
        assertFalse(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("D"))));
    }

    @Test
    void findNotesUpward_respectsLimit() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.save(sampleNote("C", List.of()));
        repository.save(sampleNote("D", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CONTAINS");
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("C"), "CONTAINS");
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("D"), "CONTAINS");

        List<MemoryNote> result = repository.findNotesUpward(new MemoryNoteId("A"), 1, 2);

        assertEquals(2, result.size());
    }

    @Test
    void findNotesUpward_filtersByRelationType() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.save(sampleNote("C", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CONTAINS");
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("C"), "RELATED_TO");

        List<MemoryNote> result = repository.findNotesUpward(new MemoryNoteId("A"), 1, 10);

        assertEquals(2, result.size());
    }

    @Test
    void findNotesUpward_avoidsCycles() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.save(sampleNote("C", List.of()));

        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CONTAINS");
        repository.addLink(new MemoryNoteId("B"), new MemoryNoteId("C"), "CONTAINS");
        repository.addLink(new MemoryNoteId("C"), new MemoryNoteId("A"), "CONTAINS");

        List<MemoryNote> result = repository.findNotesUpward(new MemoryNoteId("A"), 5, 10);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("B"))));
        assertTrue(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("C"))));
        assertFalse(result.stream().anyMatch(n -> n.id().equals(new MemoryNoteId("A"))));
    }

    @Test
    void findAllTags_returnsUniqueTags() {
        repository.save(sampleNote("n1", List.of("architecture", "backend")));
        repository.save(sampleNote("n2", List.of("architecture", "frontend")));
        repository.save(sampleNote("n3", List.of("decision")));

        Set<String> tags = repository.findAllTags();

        assertEquals(4, tags.size());
        assertTrue(tags.containsAll(Set.of("architecture", "backend", "frontend", "decision")));
    }

    @Test
    void findAllTags_whenEmpty_returnsEmptySet() {
        assertTrue(repository.findAllTags().isEmpty());
    }

    @Test
    void incrementRetrievalCount() {
        repository.save(sampleNote("note-1", List.of()));

        repository.incrementRetrievalCount(new MemoryNoteId("note-1"));
        repository.incrementRetrievalCount(new MemoryNoteId("note-1"));

        MemoryNote note = repository.findById(new MemoryNoteId("note-1")).orElseThrow();
        assertEquals(2, note.retrievalCount());
    }

    @Test
    void getGraph_returnsAllNotesAndLinks() {
        repository.save(sampleNote("A", List.of()));
        repository.save(sampleNote("B", List.of()));
        repository.addLink(new MemoryNoteId("A"), new MemoryNoteId("B"), "CALLS");

        Map<String, Object> graph = repository.getGraph();

        List<?> notes = (List<?>) graph.get("notes");
        List<?> links = (List<?>) graph.get("links");

        assertEquals(2, notes.size());
        assertEquals(1, links.size());
    }
}
