package com.thecookiezen.archiledger.infrastructure.mcp;

import com.thecookiezen.archiledger.application.service.MemoryNoteService;
import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.SimilarityResult;
import com.thecookiezen.archiledger.infrastructure.mcp.dto.MemoryNoteDto;
import com.thecookiezen.archiledger.infrastructure.mcp.dto.NoteLinkDto;
import com.thecookiezen.archiledger.infrastructure.mcp.dto.NoteLinksDto;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class McpToolAdapter {

        private final MemoryNoteService memoryNoteService;

        public McpToolAdapter(MemoryNoteService memoryNoteService) {
                this.memoryNoteService = memoryNoteService;
        }

        @Tool(name = "create_notes", description = "Create one or more memory notes. Each note is an atomic unit of knowledge with content, keywords, tags, and optional links to other notes.")
        public List<MemoryNoteDto> createNotes(
                        @ToolParam(description = "List of memory notes to create") List<MemoryNoteDto> notes) {
                return memoryNoteService.createNotes(
                                notes.stream().map(MemoryNoteDto::toDomain).toList()).stream()
                                .map(MemoryNoteDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "add_links", description = "Add typed links between existing memory notes. Links represent connections with a relation type (e.g., 'DEPENDS_ON', 'RELATED_TO', 'CONTRADICTS').")
        public void addLinks(
                        @ToolParam(description = "List of links to create, each with source note ID, target note ID, and relation type") List<NoteLinksDto> links) {
                for (NoteLinksDto link : links) {
                        for (NoteLinkDto noteLink : link.links()) {
                                memoryNoteService.addLink(
                                                new MemoryNoteId(link.fromNoteId()),
                                                new MemoryNoteId(noteLink.target()),
                                                noteLink.relationType());
                        }
                }
        }

        @Tool(name = "get_note", description = "Retrieve a specific memory note by its ID. Returns the note with its content, keywords, tags, links, and metadata. Increments the retrieval counter for relevance tracking.")
        public Optional<MemoryNoteDto> getNote(
                        @ToolParam(description = "ID of the note to retrieve") String noteId) {
                return memoryNoteService.getNote(new MemoryNoteId(noteId))
                                .map(MemoryNoteDto::fromDomain);
        }

        @Tool(name = "get_notes_by_tag", description = "Find all memory notes with a given tag. Useful for retrieving notes of a specific category (e.g., 'architecture', 'bug', 'decision').")
        public List<MemoryNoteDto> getNotesByTag(
                        @ToolParam(description = "Tag to search for (e.g., 'architecture', 'decision')") String tag) {
                return memoryNoteService.getNotesByTag(tag).stream()
                                .map(MemoryNoteDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "get_linked_notes", description = "Find all notes directly connected to a given note. Returns notes that are either linked from or linked to the specified note.")
        public List<MemoryNoteDto> getLinkedNotes(
                        @ToolParam(description = "ID of the note to find connections for") String noteId) {
                return memoryNoteService.getLinkedNotes(new MemoryNoteId(noteId)).stream()
                                .map(MemoryNoteDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "get_linked_notes_by_type", description = "Find notes connected to a given note with a specific relation type. Useful for filtering by relationship kind (e.g., 'DEPENDS_ON', 'CONTAINS', 'RELATED_TO').")
        public List<MemoryNoteDto> getLinkedNotesByType(
                        @ToolParam(description = "ID of the note to find connections for") String noteId,
                        @ToolParam(description = "Relation type to filter by (e.g., 'DEPENDS_ON', 'CONTAINS', 'RELATED_TO')") String relationType,
                        @ToolParam(description = "Maximum number of notes to return") int limit) {
                return memoryNoteService.getLinkedNotes(new MemoryNoteId(noteId), relationType, limit).stream()
                                .map(MemoryNoteDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "get_notes_upward", description = "Traverse the graph upward from a note. Performs multi-hop traversal to find all reachable notes within maxHops distance.")
        public List<MemoryNoteDto> getNotesUpward(
                        @ToolParam(description = "ID of the starting note") String noteId,
                        @ToolParam(description = "Maximum number of hops to traverse") int maxHops,
                        @ToolParam(description = "Maximum number of notes to return") int limit) {
                return memoryNoteService.getNotesUpward(new MemoryNoteId(noteId), maxHops, limit).stream()
                                .map(MemoryNoteDto::fromDomain)
                                .collect(Collectors.toList());
        }

        @Tool(name = "search_notes", description = "Perform a semantic similarity search across all memory notes. Returns the most relevant notes based on vector embeddings of their content.")
        public List<SimilarityResult<MemoryNote>> searchNotes(
                        @ToolParam(description = "Natural language query to search for similar notes") String query) {
                return memoryNoteService.similaritySearch(query);
        }

        @Tool(name = "delete_notes", description = "Delete one or more memory notes by their IDs. Also removes associated links and embeddings.")
        public void deleteNotes(
                        @ToolParam(description = "List of note IDs to delete") List<String> noteIds) {
                memoryNoteService.deleteNotes(
                                noteIds.stream().map(MemoryNoteId::new).collect(Collectors.toList()));
        }

        @Tool(name = "delete_links", description = "Remove typed links between memory notes.")
        public void deleteLinks(
                        @ToolParam(description = "Source note ID") String fromNoteId,
                        @ToolParam(description = "List of links to remove") List<NoteLinkDto> links) {
                for (NoteLinkDto link : links) {
                        memoryNoteService.removeLink(
                                        new MemoryNoteId(fromNoteId),
                                        new MemoryNoteId(link.target()),
                                        link.relationType());
                }
        }

        @Tool(name = "read_graph", description = "Read the entire knowledge graph. Returns all memory notes and their links.")
        public Map<String, Object> readGraph() {
                return memoryNoteService.readGraph();
        }

        @Tool(name = "get_all_tags", description = "List all unique tags currently used across all memory notes. Useful for discovering available categories.")
        public List<String> getAllTags() {
                return memoryNoteService.getAllTags().stream().toList();
        }
}