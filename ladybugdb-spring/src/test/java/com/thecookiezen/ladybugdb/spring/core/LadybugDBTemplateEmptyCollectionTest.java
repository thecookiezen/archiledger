package com.thecookiezen.ladybugdb.spring.core;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.thecookiezen.ladybugdb.spring.connection.SimpleConnectionFactory;
import com.thecookiezen.ladybugdb.spring.mapper.ValueMappers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that LadybugDBTemplate correctly handles empty collections in query
 * parameters.
 * <p>
 * Regression test for a SIGSEGV crash caused by {@code Native.lbugCreateList}
 * returning null when given an empty array. The null Value was passed into
 * JNI code which tried to call {@code GetLongField} on it, crashing the JVM.
 */
class LadybugDBTemplateEmptyCollectionTest {

    private LadybugDBTemplate template;
    private static SimpleConnectionFactory connectionFactory;
    private static Database db;

    @BeforeAll
    static void setupAll() {
        db = new Database(":memory:");
        connectionFactory = new SimpleConnectionFactory(db);
    }

    @BeforeEach
    void setup() {
        template = new LadybugDBTemplate(connectionFactory);
        try (Connection conn = new Connection(db)) {
            conn.query(
                    "CREATE NODE TABLE IF NOT EXISTS Entity(name STRING PRIMARY KEY, type STRING, observations STRING[])");
        }
    }

    @AfterEach
    void tearDown() {
        template.execute("MATCH (n:Entity) DETACH DELETE n");
    }

    @AfterAll
    static void tearDownAll() {
        if (connectionFactory != null) {
            connectionFactory.close();
        }
        if (db != null) {
            db.close();
        }
    }

    @Test
    void execute_shouldHandleEmptyListParameter() {
        List<String> emptyObservations = new ArrayList<>();

        assertDoesNotThrow(() -> template.execute(
                "CREATE (e:Entity {name: $name, type: $type, observations: $observations})",
                Map.of("name", "test-entity", "type", "concept", "observations", emptyObservations)));

        List<String> names = template.queryForStringList(
                "MATCH (e:Entity) RETURN e.name AS name", "name");
        assertEquals(1, names.size());
        assertEquals("test-entity", names.get(0));
    }

    @Test
    void execute_shouldHandleNonEmptyListParameter() {
        List<String> observations = List.of("first observation", "second observation");

        assertDoesNotThrow(() -> template.execute(
                "CREATE (e:Entity {name: $name, type: $type, observations: $observations})",
                Map.of("name", "test-entity", "type", "concept", "observations", observations)));

        List<String> names = template.queryForStringList(
                "MATCH (e:Entity) RETURN e.name AS name", "name");
        assertEquals(1, names.size());
        assertEquals("test-entity", names.get(0));
        assertEquals(2, template
                .queryForStringList("MATCH (e:Entity) RETURN e.observations AS observations", "observations").size());
    }

    @Test
    void queryForObject_shouldHandleEmptyListParameterInMerge() {
        List<String> emptyObservations = new ArrayList<>();

        Optional<EntityRecord> result = template.queryForObject(
                "MERGE (e:Entity {name: $name}) SET e.type = $type, e.observations = $observations RETURN e",
                Map.of("name", "merge-entity", "type", "concept", "observations", emptyObservations),
                row -> {
                    var e = row.getNode("e");
                    return new EntityRecord(
                            ValueMappers.asString(e.get("name")),
                            ValueMappers.asString(e.get("type")));
                });

        assertTrue(result.isPresent());
        assertEquals("merge-entity", result.get().name());
        assertEquals("concept", result.get().type());
    }

    @Test
    void execute_shouldHandleNullListParameter() {
        assertDoesNotThrow(() -> template.execute(
                "CREATE (e:Entity {name: $name, type: $type, observations: $observations})",
                Map.of("name", "null-entity", "type", "concept", "observations", "null-placeholder")));
    }

    record EntityRecord(String name, String type) {
    }
}
