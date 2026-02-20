package com.thecookiezen.ladybugdb.spring.repository.query;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.thecookiezen.ladybugdb.spring.annotation.NodeEntity;
import com.thecookiezen.ladybugdb.spring.annotation.Query;
import com.thecookiezen.ladybugdb.spring.annotation.RelationshipEntity;
import com.thecookiezen.ladybugdb.spring.connection.SimpleConnectionFactory;
import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.mapper.EntityWriter;
import com.thecookiezen.ladybugdb.spring.mapper.RowMapper;
import com.thecookiezen.ladybugdb.spring.mapper.ValueMappers;
import com.thecookiezen.ladybugdb.spring.repository.NodeRepository;

import com.thecookiezen.ladybugdb.spring.repository.support.EntityRegistry;
import com.thecookiezen.ladybugdb.spring.repository.support.LadybugDBRepositoryFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LadybugRepositoryQueryTest {

    private static Database db;
    private static SimpleConnectionFactory connectionFactory;
    private static LadybugDBTemplate template;
    private static TestRepository repository;

    @BeforeAll
    static void setupAll() {
        db = new Database(":memory:");
        connectionFactory = new SimpleConnectionFactory(db);
        template = new LadybugDBTemplate(connectionFactory);

        EntityRegistry registry = new EntityRegistry();
        registry.registerDescriptor(Person.class, personReader, personWriter);
        registry.registerDescriptor(Follows.class, followsReader, followsWriter);

        LadybugDBRepositoryFactory factory = new LadybugDBRepositoryFactory(template, registry);
        repository = factory.getRepository(TestRepository.class);

        try (Connection conn = new Connection(db)) {
            conn.query("CREATE NODE TABLE Person(name STRING PRIMARY KEY, age INT64)");
            conn.query("CREATE REL TABLE FOLLOWS(FROM Person TO Person, name STRING, since INT64)");
        }
    }

    @AfterEach
    void tearDown() {
        template.execute("MATCH (n:Person) DETACH DELETE n");
    }

    @AfterAll
    static void tearDownAll() {
        if (connectionFactory != null)
            connectionFactory.close();
        if (db != null)
            db.close();
    }

    @Test
    void testQueryReturningList() {
        template.execute("CREATE (p:Person {name: $name, age: $age})", Map.of("name", "Alice", "age", 30));
        template.execute("CREATE (p:Person {name: $name, age: $age})", Map.of("name", "Bob", "age", 25));

        List<Person> people = repository.findAllPeople();
        assertEquals(2, people.size());
        assertTrue(people.stream().anyMatch(p -> p.name.equals("Alice")));
        assertTrue(people.stream().anyMatch(p -> p.name.equals("Bob")));
    }

    @Test
    void testQueryReturningSingleObject() {
        template.execute("CREATE (p:Person {name: $name, age: $age})", Map.of("name", "Charlie", "age", 35));

        Person person = repository.findByName("Charlie");
        assertNotNull(person);
        assertEquals("Charlie", person.name);
    }

    @Test
    void testQueryReturningOptional_Present() {
        template.execute("CREATE (p:Person {name: $name, age: $age})", Map.of("name", "Dave", "age", 40));

        Optional<Person> person = repository.findOptionalByName("Dave");
        assertTrue(person.isPresent());
        assertEquals("Dave", person.get().name);
    }

    @Test
    void testQueryReturningOptional_Empty() {
        Optional<Person> person = repository.findOptionalByName("NonExistent");
        assertTrue(person.isEmpty());
    }

    @Test
    void testQueryParameterBinding() {
        template.execute("CREATE (p:Person {name: $name, age: $age})", Map.of("name", "Eve", "age", 28));

        Person person = repository.findByAge(28);
        assertNotNull(person);
        assertEquals("Eve", person.name);
    }

    @Test
    void testQueryMultipleParameters() {
        template.execute("CREATE (p:Person {name: $name, age: $age})", Map.of("name", "Frank", "age", 45));

        Person person = repository.findByNameAndAge("Frank", 45);
        assertNotNull(person);
        assertEquals("Frank", person.name);
    }

    @Test
    void testQueryNullParameter() {
        Person p = repository.findByName(null);
        assertNull(p);
    }

    @Test
    void testQueryReturningScalarList() {
        template.execute("CREATE (p:Person {name: $name, age: $age})", Map.of("name", "Grace", "age", 32));
        template.execute("CREATE (p:Person {name: $name, age: $age})", Map.of("name", "Henry", "age", 50));

        List<String> names = repository.findAllNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("Grace"));
        assertTrue(names.contains("Henry"));
    }

    @Test
    void testQueryReturningScalar() {
        template.execute("CREATE (p:Person {name: $name, age: $age})", Map.of("name", "Ivan", "age", 55));

        String name = repository.findNameByAge(55);
        assertEquals("Ivan", name);
    }

    interface TestRepository extends NodeRepository<Person, String, Follows, Person> {
        @Query("MATCH (n:Person) RETURN n")
        List<Person> findAllPeople();

        @Query("MATCH (n:Person) WHERE n.name = $name RETURN n")
        Person findByName(@Param("name") String name);

        @Query("MATCH (n:Person) WHERE n.name = $name RETURN n")
        Optional<Person> findOptionalByName(@Param("name") String name);

        @Query("MATCH (n:Person) WHERE n.age = $age RETURN n")
        Person findByAge(@Param("age") int age);

        @Query("MATCH (n:Person) WHERE n.name = $name AND n.age = $age RETURN n")
        Person findByNameAndAge(@Param("name") String name, @Param("age") int age);

        @Query("MATCH (n:Person) RETURN n.name")
        List<String> findAllNames();

        @Query("MATCH (n:Person) WHERE n.age = $age RETURN n.name")
        String findNameByAge(@Param("age") int age);
    }

    @NodeEntity(label = "Person")
    static class Person {
        @Id
        String name;
        int age;

        Person() {
        }

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    static RowMapper<Person> personReader = (row) -> {
        var p = row.getNode("n");
        String name = ValueMappers.asString(p.get("name"));
        int age = ValueMappers.asInteger(p.get("age"));
        return new Person(name, age);
    };

    static EntityWriter<Person> personWriter = (entity) -> {
        return Map.of("age", entity.age);
    };

    @RelationshipEntity(type = "FOLLOWS", nodeType = Person.class, sourceField = "from", targetField = "to")
    static class Follows {
        @Id
        String name;
        Person from;
        Person to;
        int since;

        Follows() {
        }
    }

    static EntityWriter<Follows> followsWriter = (entity) -> Map.of();
    static RowMapper<Follows> followsReader = (row) -> new Follows();
}
