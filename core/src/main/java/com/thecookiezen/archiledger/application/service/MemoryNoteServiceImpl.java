package com.thecookiezen.archiledger.application.service;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.repository.EmbeddingsService;
import com.thecookiezen.archiledger.domain.repository.MemoryNoteRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
class MemoryNoteServiceImpl implements MemoryNoteService {

    private final MemoryNoteRepository repository;
    private final EmbeddingsService embeddingsService;

    MemoryNoteServiceImpl(MemoryNoteRepository repository, EmbeddingsService embeddingsService) {
        this.repository = repository;
        this.embeddingsService = embeddingsService;
    }

    @Override
    public MemoryNote createNote(MemoryNote note) {
        float[] embedding = embeddingsService.generateEmbeddings(note);
        return repository.save(note.withEmbedding(embedding));
    }

    @Override
    public List<MemoryNote> createNotes(List<MemoryNote> notes) {
        List<MemoryNote> created = new ArrayList<>();
        for (MemoryNote note : notes) {
            created.add(createNote(note));
        }
        return created;
    }

    @Override
    public Optional<MemoryNote> getNote(MemoryNoteId id) {
        Optional<MemoryNote> note = repository.findById(id);
        note.ifPresent(n -> repository.incrementRetrievalCount(id));
        return note;
    }

    @Override
    public List<MemoryNote> getAllNotes() {
        return repository.findAll();
    }

    @Override
    public void deleteNote(MemoryNoteId id) {
        repository.delete(id);
    }

    @Override
    public void deleteNotes(List<MemoryNoteId> ids) {
        for (MemoryNoteId id : ids) {
            deleteNote(id);
        }
    }

    @Override
    public void addLink(MemoryNoteId from, MemoryNoteId to, String relationType) {
        repository.addLink(from, to, relationType);
    }

    @Override
    public void removeLink(MemoryNoteId from, MemoryNoteId to, String relationType) {
        repository.removeLink(from, to, relationType);
    }

    @Override
    public List<MemoryNote> getNotesByTag(String tag) {
        return repository.findByTag(tag);
    }

    @Override
    public List<MemoryNote> getLinkedNotes(MemoryNoteId noteId) {
        return repository.findLinkedNotes(noteId);
    }

    @Override
    public Set<String> getAllTags() {
        return repository.findAllTags();
    }

    @Override
    public Map<String, Object> readGraph() {
        return repository.getGraph();
    }

    @Override
    public List<String> similaritySearch(String query) {
        float[] queryEmbedding = embeddingsService.embed(query);
        return repository.findSimilar(queryEmbedding, 10).stream()
                .map(repository::findById)
                .flatMap(Optional::stream)
                .map(MemoryNote::content)
                .toList();
    }
}
