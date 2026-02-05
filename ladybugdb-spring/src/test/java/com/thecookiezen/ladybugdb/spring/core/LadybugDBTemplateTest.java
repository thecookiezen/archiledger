package com.thecookiezen.ladybugdb.spring.core;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.thecookiezen.ladybugdb.spring.connection.SimpleConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LadybugDBTemplateTest {

    private LadybugDBTemplate template;
    private SimpleConnectionFactory connectionFactory;
    private Database db;

    @BeforeEach
    void setup() {
        db = new Database(":memory:");
        try (Connection conn = new Connection(db)) {
            conn.query("CREATE NODE TABLE Person(name STRING PRIMARY KEY, age INT64)");
        }
        connectionFactory = new SimpleConnectionFactory(db);
        template = new LadybugDBTemplate(connectionFactory);
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.close();
        }
    }

    @Test
    void execute_shouldRunWriteQuery() {
        template.execute("CREATE (p:Person {name: 'Alice', age: 30})");

        List<String> names = template.queryForStringList("MATCH (p:Person) RETURN p.name", 0);
        assertEquals(1, names.size());
        assertEquals("Alice", names.get(0));
    }

    @Test
    void query_shouldMapResultsUsingRowMapper() {
        template.execute("CREATE (p:Person {name: 'Bob', age: 25})");
        template.execute("CREATE (p:Person {name: 'Charlie', age: 35})");

        List<PersonRecord> people = template.query(
                "MATCH (p:Person) RETURN p.name, p.age ORDER BY p.name",
                (row, rowNum) -> new PersonRecord(
                        row.getValue(0).getValue().toString(),
                        Integer.parseInt(row.getValue(1).getValue().toString())));

        assertEquals(2, people.size());
        assertEquals("Bob", people.get(0).name());
        assertEquals(25, people.get(0).age());
        assertEquals("Charlie", people.get(1).name());
        assertEquals(35, people.get(1).age());
    }

    @Test
    void queryForObject_shouldReturnSingleResult() {
        template.execute("CREATE (p:Person {name: 'David', age: 40})");

        Optional<PersonRecord> result = template.queryForObject(
                "MATCH (p:Person) WHERE p.name = 'David' RETURN p.name, p.age",
                (row, rowNum) -> new PersonRecord(
                        row.getValue(0).getValue().toString(),
                        Integer.parseInt(row.getValue(1).getValue().toString())));

        assertTrue(result.isPresent());
        assertEquals("David", result.get().name());
        assertEquals(40, result.get().age());
    }

    @Test
    void queryForObject_shouldReturnEmptyWhenNoResults() {
        Optional<PersonRecord> result = template.queryForObject(
                "MATCH (p:Person) WHERE p.name = 'NonExistent' RETURN p.name, p.age",
                (row, rowNum) -> new PersonRecord(
                        row.getValue(0).getValue().toString(),
                        Integer.parseInt(row.getValue(1).getValue().toString())));

        assertTrue(result.isEmpty());
    }

    @Test
    void queryForStringList_shouldReturnListOfStrings() {
        template.execute("CREATE (p:Person {name: 'Eve', age: 28})");
        template.execute("CREATE (p:Person {name: 'Frank', age: 33})");

        List<String> names = template.queryForStringList(
                "MATCH (p:Person) RETURN p.name ORDER BY p.name", 0);

        assertEquals(2, names.size());
        assertEquals("Eve", names.get(0));
        assertEquals("Frank", names.get(1));
    }

    @Test
    void query_shouldProvideRowNumberToMapper() {
        template.execute("CREATE (p:Person {name: 'G1', age: 1})");
        template.execute("CREATE (p:Person {name: 'G2', age: 2})");
        template.execute("CREATE (p:Person {name: 'G3', age: 3})");

        List<Integer> rowNumbers = template.query(
                "MATCH (p:Person) RETURN p.name ORDER BY p.name",
                (row, rowNum) -> rowNum);

        assertEquals(List.of(0, 1, 2), rowNumbers);
    }

    record PersonRecord(String name, int age) {
    }
}
