package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb;

import java.util.List;

import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugMemoryNote;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LadybugNoteLink;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.LinkProjection;
import com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model.SimilarityResultProjection;
import com.thecookiezen.ladybugdb.spring.annotation.Query;
import com.thecookiezen.ladybugdb.spring.repository.NodeRepository;

public interface MemoryNoteDbRepository
                extends NodeRepository<LadybugMemoryNote, String, LadybugNoteLink, LadybugMemoryNote> {

        @Query("MATCH (n:MemoryNote) WHERE list_contains(n.tags, $tag) RETURN n")
        List<LadybugMemoryNote> findByTag(String tag);

        @Query("MATCH (source:MemoryNote)-[r:LINKED_TO]->(target:MemoryNote) WHERE source.id = $noteId OR target.id = $noteId RETURN source.id AS fromId, target.id AS toId, r.relationType AS relationType, r.context AS context")
        List<LinkProjection> findLinksForNote(String noteId);

        @Query("MATCH (source:MemoryNote)-[r:LINKED_TO]->(target:MemoryNote) WHERE source.id = $noteId RETURN source.id AS fromId, target.id AS toId, r.relationType AS relationType, r.context AS context")
        List<LinkProjection> findLinksFrom(String noteId);

        @Query("MATCH (source:MemoryNote)-[r:LINKED_TO]->(target:MemoryNote) WHERE r.relationType = $relationType RETURN source.id AS fromId, target.id AS toId, r.relationType AS relationType, r.context AS context")
        List<LinkProjection> findLinksByRelationType(String relationType);

        @Query("MATCH (n:MemoryNote)-[r:LINKED_TO]-(m:MemoryNote) WHERE n.id = $noteId RETURN DISTINCT m AS n")
        List<LadybugMemoryNote> findLinkedNotes(String noteId);

        @Query("MATCH (n:MemoryNote)-[r:LINKED_TO]-(m:MemoryNote) WHERE n.id = $noteId AND r.relationType = $relationType RETURN DISTINCT m as n LIMIT $limit")
        List<LadybugMemoryNote> findLinkedNotes(String noteId, String relationType, int limit);

        @Query("MATCH (n:MemoryNote) UNWIND n.tags AS tag RETURN DISTINCT tag")
        List<String> findAllTags();

        @Query("MATCH (source:MemoryNote)-[r:LINKED_TO]->(target:MemoryNote) RETURN source.id AS fromId, target.id AS toId, r.relationType AS relationType, r.context AS context")
        List<LinkProjection> findAllLinks();

        @Query(value = "CALL QUERY_VECTOR_INDEX('MemoryNote', 'note_embedding_idx', $queryVector, $limit) YIELD node, distance RETURN node AS n, distance AS score ORDER BY distance", loadExtensions = {
                        "vector" })
        List<SimilarityResultProjection> findSimilarRaw(float[] queryVector, long limit);

        @Query("MATCH (n:MemoryNote) DETACH DELETE n")
        void deleteAllNotes();

        @Query("MATCH (n:MemoryNote {id: $noteId}) RETURN n.embedding IS NOT NULL")
        Boolean hasEmbedding(String noteId);

        /**
         * Writes the note's embedding in place on the note node: SET creates the
         * property on first write and updates it on later saves, so the live HNSW
         * index on {@code MemoryNote.embedding} sees the new vector (supported
         * since LadybugDB 0.18.0). Callers gate this on the content actually
         * having changed — the property is deliberately NOT part of the mapped
         * entity, so the generic save path never touches the index.
         */
        @Query(value = "MATCH (n:MemoryNote {id: $noteId}) SET n.embedding = $embedding", loadExtensions = {
                        "vector" })
        void setEmbedding(String noteId, float[] embedding);
}
