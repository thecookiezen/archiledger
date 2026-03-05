package com.thecookiezen.archiledger.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelValidationTest {

    @Nested
    @DisplayName("MemoryNoteId Validation")
    class MemoryNoteIdTest {
        @Test
        void shouldCreateValidMemoryNoteId() {
            MemoryNoteId id = new MemoryNoteId("note-1");
            assertEquals("note-1", id.value());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t", "\n" })
        void shouldThrowExceptionForInvalidMemoryNoteId(String invalidValue) {
            Exception exception = assertThrows(IllegalArgumentException.class, () -> new MemoryNoteId(invalidValue));
            assertEquals("MemoryNoteId cannot be null or blank", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("NoteLink Validation")
    class NoteLinkTest {
        @Test
        void shouldCreateValidNoteLink() {
            NoteLink link = new NoteLink("target-note", "DEPENDS_ON");
            assertEquals("target-note", link.target().value());
            assertEquals("DEPENDS_ON", link.relationType());
        }

        @Test
        void shouldThrowExceptionWhenTargetIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> new NoteLink((MemoryNoteId) null, "DEPENDS_ON"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t", "\n" })
        void shouldThrowExceptionForInvalidRelationType(String invalidValue) {
            assertThrows(IllegalArgumentException.class,
                    () -> new NoteLink("target", invalidValue));
        }
    }

    @Nested
    @DisplayName("MemoryNote Validation")
    class MemoryNoteTest {
        @Test
        void shouldCreateValidMemoryNote() {
            MemoryNote note = new MemoryNote(
                    new MemoryNoteId("note-1"),
                    "Some knowledge content",
                    List.of("java", "architecture"),
                    "project-x",
                    List.of("design", "backend"),
                    List.of(new NoteLink("note-2", "RELATED_TO")),
                    "2026-03-04T16:00:00Z",
                    0,
                    null);

            assertEquals("note-1", note.id().value());
            assertEquals("Some knowledge content", note.content());
            assertEquals(2, note.keywords().size());
            assertEquals("project-x", note.context());
            assertEquals(2, note.tags().size());
            assertEquals(1, note.links().size());
            assertEquals("2026-03-04T16:00:00Z", note.timestamp());
            assertEquals(0, note.retrievalCount());
        }

        @Test
        void shouldHandleNullKeywordsTagsAndLinks() {
            MemoryNote note = new MemoryNote(
                    new MemoryNoteId("note-1"),
                    "content",
                    null, null, null, null,
                    "2026-03-04T16:00:00Z",
                    0,
                    null);

            assertNotNull(note.keywords());
            assertTrue(note.keywords().isEmpty());
            assertNotNull(note.tags());
            assertTrue(note.tags().isEmpty());
            assertNotNull(note.links());
            assertTrue(note.links().isEmpty());
        }

        @Test
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MemoryNote(null, "content", List.of(), null, List.of(), List.of(),
                            "2026-03-04T16:00:00Z", 0, null));
        }

        @Test
        void shouldThrowExceptionWhenContentIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MemoryNote(new MemoryNoteId("id"), null, List.of(), null, List.of(), List.of(),
                            "2026-03-04T16:00:00Z", 0, null));
        }

        @Test
        void shouldThrowExceptionWhenTimestampIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MemoryNote(new MemoryNoteId("id"), "content", List.of(), null, List.of(), List.of(),
                            null, 0, null));
        }

        @Test
        void shouldThrowExceptionWhenRetrievalCountIsNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MemoryNote(new MemoryNoteId("id"), "content", List.of(), null, List.of(), List.of(),
                            "2026-03-04T16:00:00Z", -1, null));
        }

        @Test
        void keywordsTagsAndLinksShouldBeImmutable() {
            MemoryNote note = new MemoryNote(
                    new MemoryNoteId("note-1"),
                    "content",
                    List.of("keyword"),
                    null,
                    List.of("tag"),
                    List.of(new NoteLink("note-2", "RELATED_TO")),
                    "2026-03-04T16:00:00Z",
                    0,
                    null);

            assertThrows(UnsupportedOperationException.class, () -> note.keywords().add("new"));
            assertThrows(UnsupportedOperationException.class, () -> note.tags().add("new"));
            assertThrows(UnsupportedOperationException.class,
                    () -> note.links().add(new NoteLink("note-3", "X")));
        }

        @Test
        void withRetrievalCountShouldReturnNewInstance() {
            MemoryNote original = new MemoryNote(
                    new MemoryNoteId("note-1"), "content", List.of(), null, List.of(), List.of(),
                    "2026-03-04T16:00:00Z", 0, null);

            MemoryNote updated = original.withRetrievalCount(5);

            assertEquals(0, original.retrievalCount());
            assertEquals(5, updated.retrievalCount());
            assertEquals(original.id(), updated.id());
            assertEquals(original.content(), updated.content());
        }

        @Test
        void withLinksShouldReturnNewInstance() {
            MemoryNote original = new MemoryNote(
                    new MemoryNoteId("note-1"), "content", List.of(), null, List.of(), List.of(),
                    "2026-03-04T16:00:00Z", 0, null);

            List<NoteLink> newLinks = List.of(new NoteLink("note-2", "RELATED_TO"));
            MemoryNote updated = original.withLinks(newLinks);

            assertTrue(original.links().isEmpty());
            assertEquals(1, updated.links().size());
            assertEquals("note-2", updated.links().get(0).target().value());
        }
    }
}
