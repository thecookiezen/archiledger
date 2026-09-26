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

        @Query(value = "CALL QUERY_VECTOR_INDEX('NoteEmbedding', 'note_embedding_idx', $queryVector, $limit) YIELD node, distance MATCH (n:MemoryNote)-[:HAS_EMBEDDING]->(node) RETURN n, distance AS score ORDER BY distance", loadExtensions = {
                        "vector" })
        List<SimilarityResultProjection> findSimilarRaw(float[] queryVector, long limit);

        @Query(value = "MATCH (e:NoteEmbedding {noteId: $noteId}) SET e.embedding = $embedding", loadExtensions = {
                        "vector" })
        void updateEmbedding(String noteId, float[] embedding);

        @Query("MATCH (ne:NoteEmbedding)-[r:HAS_EMBEDDING]-(mn:MemoryNote) DETACH DELETE mn, ne")
        void deleteAllNotesWithEmbeddings();

        /**
         * First-time insert of an embedding node for a note. Updates of an
         * existing embedding go through {@link #updateEmbedding(String, float[])},
         * which SETs the vector in place on the live HNSW index instead of
         * delete+recreating the node.
         */
        @Query(value = "MATCH (n:MemoryNote {id: $noteId}) CREATE (n)-[:HAS_EMBEDDING]->(e:NoteEmbedding {noteId: $noteId, embedding: $embedding})", loadExtensions = {
                        "vector" })
        void saveEmbedding(String noteId, float[] embedding);
}
