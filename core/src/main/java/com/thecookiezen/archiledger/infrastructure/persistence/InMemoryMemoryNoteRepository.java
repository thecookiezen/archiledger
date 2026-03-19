package com.thecookiezen.archiledger.infrastructure.persistence;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.NoteLink;
import com.thecookiezen.archiledger.domain.model.SimilarityResult;
import com.thecookiezen.archiledger.domain.repository.MemoryNoteRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Repository
@Profile("default")
class InMemoryMemoryNoteRepository implements MemoryNoteRepository {

    private final Map<MemoryNoteId, MemoryNote> notes = new ConcurrentHashMap<>();
    private final List<StoredLink> links = new CopyOnWriteArrayList<>();

    @Override
    public MemoryNote save(MemoryNote note) {
        notes.put(note.id(), note);
        return note;
    }

    @Override
    public Optional<MemoryNote> findById(MemoryNoteId id) {
        return Optional.ofNullable(notes.get(id))
                .map(note -> note.withLinks(findLinksFrom(id)));
    }

    @Override
    public List<MemoryNote> findAll() {
        return notes.values().stream()
                .map(note -> note.withLinks(findLinksFrom(note.id())))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(MemoryNoteId id) {
        notes.remove(id);
        links.removeIf(link -> link.from.equals(id) || link.to.equals(id));
    }

    @Override
    public void addLink(MemoryNoteId from, MemoryNoteId to, String relationType) {
        boolean exists = links.stream()
                .anyMatch(link -> link.from.equals(from) && link.to.equals(to)
                        && link.relationType.equals(relationType));
        if (!exists) {
            links.add(new StoredLink(from, to, relationType));
        }
    }

    @Override
    public void removeLink(MemoryNoteId from, MemoryNoteId to, String relationType) {
        links.removeIf(link -> link.from.equals(from) && link.to.equals(to)
                && link.relationType.equals(relationType));
    }

    @Override
    public List<NoteLink> findLinksFrom(MemoryNoteId id) {
        return links.stream()
                .filter(link -> link.from.equals(id))
                .map(link -> new NoteLink(link.to, link.relationType))
                .collect(Collectors.toList());
    }

    @Override
    public List<MemoryNote> findByTag(String tag) {
        return notes.values().stream()
                .filter(note -> note.tags().contains(tag))
                .map(note -> note.withLinks(findLinksFrom(note.id())))
                .collect(Collectors.toList());
    }

    @Override
    public List<MemoryNote> findLinkedNotes(MemoryNoteId noteId) {
        Set<MemoryNoteId> linkedIds = new HashSet<>();
        for (StoredLink link : links) {
            if (link.from.equals(noteId)) {
                linkedIds.add(link.to);
            } else if (link.to.equals(noteId)) {
                linkedIds.add(link.from);
            }
        }
        return linkedIds.stream()
                .map(notes::get)
                .filter(note -> note != null)
                .map(note -> note.withLinks(findLinksFrom(note.id())))
                .collect(Collectors.toList());
    }

    @Override
    public List<MemoryNote> findLinkedNotes(MemoryNoteId noteId, String relationType, int limit) {
        return links.stream()
                .filter(link -> (link.from.equals(noteId) || link.to.equals(noteId))
                        && link.relationType.equals(relationType))
                .limit(limit)
                .map(link -> link.from.equals(noteId) ? link.to : link.from)
                .map(id -> notes.get(id))
                .toList();
    }

    @Override
    public List<MemoryNote> findNotesUpward(MemoryNoteId noteId, int maxHops, int limit) {
        Set<MemoryNoteId> visited = new HashSet<>();
        visited.add(noteId);
        List<MemoryNoteId> current = List.of(noteId);
        for (int hop = 0; hop < maxHops && !current.isEmpty(); hop++) {
            List<MemoryNoteId> next = new ArrayList<>();
            for (MemoryNoteId currentId : current) {
                for (StoredLink link : links) {
                    MemoryNoteId target = null;
                    if (link.from.equals(currentId)) {
                        target = link.to;
                    } else if (link.to.equals(currentId)) {
                        target = link.from;
                    }
                    if (target != null && !visited.contains(target)) {
                        visited.add(target);
                        next.add(target);
                    }
               }
            }
            current = next;
        }
        visited.remove(noteId);
        return visited.stream().map(id -> notes.get(id)).limit(limit).toList();
    }

    @Override
    public Set<String> findAllTags() {
        return notes.values().stream()
                .flatMap(note -> note.tags().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, Object> getGraph() {
        return Map.of(
                "notes", findAll(),
                "links", new ArrayList<>(links.stream()
                        .map(l -> new NoteLink(l.to, l.relationType))
                        .toList()));
    }

    @Override
    public void incrementRetrievalCount(MemoryNoteId id) {
        notes.computeIfPresent(id, (key, note) -> note.withRetrievalCount(note.retrievalCount() + 1));
    }

    @Override
    public List<SimilarityResult<MemoryNote>> findSimilar(float[] queryEmbedding, int topK) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return List.of();
        }

        record ScoredNote(MemoryNoteId id, double score) {
        }

        return notes.values().stream()
                .filter(note -> note.embedding() != null && note.embedding().length == queryEmbedding.length)
                .map(note -> new ScoredNote(note.id(), cosineSimilarity(queryEmbedding, note.embedding())))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .map(scored -> new SimilarityResult<>(findById(scored.id()).orElseThrow(), scored.score()))
                .collect(Collectors.toList());
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }

    private record StoredLink(MemoryNoteId from, MemoryNoteId to, String relationType) {
    }
}
