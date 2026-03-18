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

        @Query("MATCH (source:MemoryNote)-[r:LINKED_TO]->(target:MemoryNote) WHERE source.id = $noteId OR target.id = $noteId RETURN source.id AS fromId, target.id AS toId, r.relationType AS relationType")
        List<LinkProjection> findLinksForNote(String noteId);

        @Query("MATCH (source:MemoryNote)-[r:LINKED_TO]->(target:MemoryNote) WHERE source.id = $noteId RETURN source.id AS fromId, target.id AS toId, r.relationType AS relationType")
        List<LinkProjection> findLinksFrom(String noteId);

        @Query("MATCH (source:MemoryNote)-[r:LINKED_TO]->(target:MemoryNote) WHERE r.relationType = $relationType RETURN source.id AS fromId, target.id AS toId, r.relationType AS relationType")
        List<LinkProjection> findLinksByRelationType(String relationType);

        @Query("MATCH (n:MemoryNote)-[r:LINKED_TO]-(m:MemoryNote) WHERE n.id = $noteId RETURN DISTINCT m AS n")
        List<LadybugMemoryNote> findLinkedNotes(String noteId);

        @Query("MATCH (n:MemoryNote) UNWIND n.tags AS tag RETURN DISTINCT tag")
        List<String> findAllTags();

        @Query("MATCH (source:MemoryNote)-[r:LINKED_TO]->(target:MemoryNote) RETURN source.id AS fromId, target.id AS toId, r.relationType AS relationType")
        List<LinkProjection> findAllLinks();

        @Query(value = "CALL QUERY_VECTOR_INDEX('NoteEmbedding', 'note_embedding_idx', $queryVector, $limit) YIELD node, distance MATCH (n:MemoryNote)-[:HAS_EMBEDDING]->(node) RETURN n AS note, distance AS score ORDER BY distance", loadExtensions = {
                        "vector" })
        List<SimilarityResultProjection> findSimilarRaw(float[] queryVector, long limit);

        @Query("MATCH (e:NoteEmbedding {noteId: $noteId}) DETACH DELETE e")
        void deleteEmbedding(String noteId);

        @Query(value = "MATCH (n:MemoryNote {id: $noteId}) CREATE (n)-[:HAS_EMBEDDING]->(e:NoteEmbedding {noteId: $noteId, embedding: $embedding})", loadExtensions = {
                        "vector" })
        void saveEmbedding(String noteId, float[] embedding);
}
