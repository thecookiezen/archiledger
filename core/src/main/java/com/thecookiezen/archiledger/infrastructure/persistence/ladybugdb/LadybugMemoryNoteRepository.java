package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb;

import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;
import com.thecookiezen.archiledger.domain.model.NoteLink;
import com.thecookiezen.archiledger.domain.repository.MemoryNoteRepository;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugMemoryNote;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugNoteLink;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LinkProjection;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@Profile("ladybugdb")
public class LadybugMemoryNoteRepository implements MemoryNoteRepository {

    private final MemoryNoteDbRepository dbRepository;

    public LadybugMemoryNoteRepository(MemoryNoteDbRepository dbRepository) {
        this.dbRepository = dbRepository;
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
            addLink(note.id(), link.target(), link.relationType());
        }

        return toDomainNote(saved);
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
                .collect(Collectors.toList());
    }

    @Override
    public void delete(MemoryNoteId id) {
        dbRepository.deleteById(id.value());
    }

    @Override
    public void addLink(MemoryNoteId from, MemoryNoteId to, String relationType) {
        LadybugMemoryNote sourceNote = dbRepository.findById(from.value())
                .orElseThrow(() -> new IllegalArgumentException("Source note not found: " + from.value()));
        LadybugMemoryNote targetNote = dbRepository.findById(to.value())
                .orElseThrow(() -> new IllegalArgumentException("Target note not found: " + to.value()));

        boolean exists = dbRepository.findLinksFrom(from.value()).stream()
                .anyMatch(link -> link.toId().equals(to.value())
                        && link.relationType().equals(relationType));

        if (!exists) {
            String linkName = from.value() + "-" + relationType + "-" + to.value();
            LadybugNoteLink link = new LadybugNoteLink(linkName, sourceNote, targetNote, relationType);
            dbRepository.createRelation(sourceNote, targetNote, link);
        }
    }

    @Override
    public void removeLink(MemoryNoteId from, MemoryNoteId to, String relationType) {
        dbRepository.findById(from.value()).ifPresent(sourceNote -> {
            List<LadybugNoteLink> matching = dbRepository.findRelationsBySource(sourceNote).stream()
                    .filter(link -> link.getTargetNote().getId().equals(to.value())
                            && link.getRelationType().equals(relationType))
                    .collect(Collectors.toList());

            for (LadybugNoteLink link : matching) {
                dbRepository.deleteRelation(link);
            }
        });
    }

    @Override
    public List<NoteLink> findLinksFrom(MemoryNoteId id) {
        return dbRepository.findLinksFrom(id.value()).stream()
                .map(this::toDomainLink)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemoryNote> findByTag(String tag) {
        return dbRepository.findByTag(tag).stream()
                .map(note -> toDomainNoteWithLinks(note, note.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MemoryNote> findLinkedNotes(MemoryNoteId noteId) {
        return dbRepository.findLinkedNotes(noteId.value()).stream()
                .map(note -> toDomainNoteWithLinks(note, note.getId()))
                .collect(Collectors.toList());
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
    public List<MemoryNoteId> findSimilar(float[] queryEmbedding, int topK) {
        return dbRepository.findSimilarRaw(queryEmbedding, topK);
    }

    private MemoryNote toDomainNote(LadybugMemoryNote note) {
        return new MemoryNote(
                new MemoryNoteId(note.getId()),
                note.getContent(),
                note.getKeywords(),
                note.getContext(),
                note.getTags(),
                List.of(),
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
        return new NoteLink(new MemoryNoteId(projection.toId()), projection.relationType());
    }
}
