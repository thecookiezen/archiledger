package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb;

import com.thecookiezen.archiledger.domain.model.LinkDefinition;
import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.NoteLink;
import com.thecookiezen.archiledger.domain.model.SimilarityResult;
import com.thecookiezen.archiledger.domain.repository.MemoryNoteRepository;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugMemoryNote;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugNoteLink;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LinkProjection;
import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
public class LadybugMemoryNoteRepository implements MemoryNoteRepository {

    private final MemoryNoteDbRepository dbRepository;
    private final LadybugDBTemplate template;

    public LadybugMemoryNoteRepository(MemoryNoteDbRepository dbRepository, LadybugDBTemplate template) {
        this.dbRepository = dbRepository;
        this.template = template;
    }

    @Override
    public MemoryNote save(MemoryNote note) {
        LadybugMemoryNote ladybugNote = dbRepository.findById(note.id().value())
                .orElse(new LadybugMemoryNote());

        ladybugNote.setId(note.id().value());
        ladybugNote.setContent(note.content());
        ladybugNote.setKeywords(note.keywords());
        ladybugNote.setContext(note.context());
        ladybugNote.setTags(note.tags());
        ladybugNote.setTimestamp(note.timestamp());
        ladybugNote.setRetrievalCount(note.retrievalCount());
        LadybugMemoryNote saved = dbRepository.save(ladybugNote);

        if (note.embedding() != null && note.embedding().length > 0) {
            dbRepository.deleteEmbedding(note.id().value());
            dbRepository.saveEmbedding(note.id().value(), note.embedding());
        }

        for (NoteLink link : note.links()) {
            addLink(new LinkDefinition(note.id(), link.target(), link.relationType(), link.context()));
        }

        return toDomainNote(saved, note.links());
    }

    @Override
    public Optional<MemoryNote> findById(MemoryNoteId id) {
        return dbRepository.findById(id.value())
                .map(note -> toDomainNoteWithLinks(note, id.value()));
    }

    @Override
    public List<MemoryNote> findAll() {
        return StreamSupport.stream(dbRepository.findAll().spliterator(), false)
                .map(note -> toDomainNoteWithLinks(note, note.getId()))
                .toList();
    }

    @Override
    public void delete(MemoryNoteId id) {
        dbRepository.deleteById(id.value());
    }

    @Override
    public void addLink(LinkDefinition link) {
        LadybugMemoryNote sourceNote = dbRepository.findById(link.source().value())
                .orElseThrow(() -> new IllegalArgumentException("Source note not found: " + link.source().value()));
        LadybugMemoryNote targetNote = dbRepository.findById(link.target().value())
                .orElseThrow(() -> new IllegalArgumentException("Target note not found: " + link.target().value()));

        boolean exists = dbRepository.findLinksFrom(link.source().value()).stream()
                .anyMatch(existingLink -> existingLink.toId().equals(link.target().value())
                        && existingLink.relationType().equals(link.relationType())
                        && (existingLink.context() != null && existingLink.context().equals(link.context())));

        if (!exists) {
            String linkName = link.source().value() + "-" + link.relationType() + "-" + link.target().value();
            LadybugNoteLink ladybugLink = new LadybugNoteLink(linkName, sourceNote, targetNote, link.relationType(), link.context());
            dbRepository.createRelation(sourceNote, targetNote, ladybugLink);
        }
    }

    @Override
    public void removeLink(MemoryNoteId from, MemoryNoteId to, String relationType) {
        dbRepository.findById(from.value()).ifPresent(sourceNote -> {
            List<LadybugNoteLink> matching = dbRepository.findRelationsBySource(sourceNote).stream()
                    .filter(link -> link.getTargetNote().getId().equals(to.value())
                            && link.getRelationType().equals(relationType))
                    .toList();

            for (LadybugNoteLink link : matching) {
                dbRepository.deleteRelation(link);
            }
        });
    }

    @Override
    public List<NoteLink> findLinksFrom(MemoryNoteId id) {
        return dbRepository.findLinksFrom(id.value()).stream()
                .map(this::toDomainLink)
                .toList();
    }

    @Override
    public List<MemoryNote> findByTag(String tag) {
        return dbRepository.findByTag(tag).stream()
                .map(note -> toDomainNoteWithLinks(note, note.getId()))
                .toList();
    }

    @Override
    public List<MemoryNote> findLinkedNotes(MemoryNoteId noteId) {
        return dbRepository.findLinkedNotes(noteId.value()).stream()
                .map(note -> toDomainNoteWithLinks(note, note.getId()))
                .toList();
    }

    @Override
    public List<MemoryNote> findLinkedNotes(MemoryNoteId noteId, String relationType, int limit) {
        return dbRepository.findLinkedNotes(noteId.value(), relationType, limit).stream()
            .map(note -> toDomainNoteWithLinks(note, note.getId()))
            .toList();
    }

    @Override
    public List<MemoryNote> findNotesUpward(MemoryNoteId noteId, int maxHops, int limit) {
        String query = """
            MATCH 
                (n:MemoryNote)-[r:LINKED_TO* acyclic 1..%d]->(m:MemoryNote) 
            WHERE
                n.id = $noteId
            RETURN 
                DISTINCT m as n 
            LIMIT
             $limit    
                """.formatted(maxHops);
        return template.query(query, Map.of("noteId", noteId.value(), "limit", limit), LadybugMemoryNote.class)
            .stream()
            .map(note -> toDomainNoteWithLinks(note, note.getId()))
            .toList();
    }

    @Override
    public Set<String> findAllTags() {
        return dbRepository.findAllTags().stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, Object> getGraph() {
        List<MemoryNote> allNotes = findAll();
        List<LinkProjection> allLinks = dbRepository.findAllLinks();
        return Map.of(
                "notes", allNotes,
                "links", allLinks.stream().map(this::toDomainLink).collect(Collectors.toList()));
    }

    @Override
    public void incrementRetrievalCount(MemoryNoteId id) {
        dbRepository.findById(id.value()).ifPresent(note -> {
            note.setRetrievalCount(note.getRetrievalCount() + 1);
            dbRepository.save(note);
        });
    }

    @Override
    public List<SimilarityResult<MemoryNote>> findSimilar(float[] queryEmbedding, int topK) {
        return findSimilar(queryEmbedding, topK, 0.0, 0.0);
    }

    @Override
    public List<SimilarityResult<MemoryNote>> findSimilar(float[] queryEmbedding, int topK, double threshold, double temperature) {
        return dbRepository.findSimilarRaw(queryEmbedding, topK).stream()
                .map(projection -> {
                    double distance = projection.score();
                    double score = applyTemperatureScaling(distance, temperature);
                    return new SimilarityResult<>(
                            toDomainNoteWithLinks(projection.note(), projection.note().getId()),
                            score);
                })
                .filter(result -> result.score() >= threshold)
                .toList();
    }

    private double applyTemperatureScaling(double distance, double temperature) {
        if (temperature <= 0.0) {
            return 1.0 - distance;
        }
        return Math.exp(-distance / temperature);
    }

    private MemoryNote toDomainNote(LadybugMemoryNote note, List<NoteLink> links) {
        return new MemoryNote(
                new MemoryNoteId(note.getId()),
                note.getContent(),
                note.getKeywords(),
                note.getContext(),
                note.getTags(),
                links,
                note.getTimestamp(),
                note.getRetrievalCount(),
                null);
    }

    private MemoryNote toDomainNoteWithLinks(LadybugMemoryNote note, String noteId) {
        List<NoteLink> links = dbRepository.findLinksFrom(noteId).stream()
                .map(this::toDomainLink)
                .collect(Collectors.toList());
        return new MemoryNote(
                new MemoryNoteId(note.getId()),
                note.getContent(),
                note.getKeywords(),
                note.getContext(),
                note.getTags(),
                links,
                note.getTimestamp(),
                note.getRetrievalCount(),
                null);
    }

    private NoteLink toDomainLink(LinkProjection projection) {
        return new NoteLink(new MemoryNoteId(projection.toId()), projection.relationType(), projection.context());
    }
}
