# LadybugDB Spring

A Spring Data-like integration framework for [LadybugDB](https://ladybugdb.com), providing familiar Spring patterns for graph database operations.

## Features

- **Repository Pattern**: Spring Data-style `NodeRepository` for CRUD operations on graph nodes
- **Template Support**: `LadybugDBTemplate` for executing Cypher queries with connection management
- **Connection Pooling**: Built-in connection pooling via `PooledConnectionFactory`
- **Cypher DSL Integration**: Use [Neo4j Cypher DSL](https://github.com/neo4j/cypher-dsl) for type-safe query building
- **Entity Mapping**: Annotation-based entity mapping with `@NodeEntity` and `@Id`
- **Custom Queries**: Support for `@Query` annotation on repository interfaces

## Quick Start

### Define an Entity

```java
@NodeEntity(label = "Person")
public class Person {
    @Id
    private String name;
    private int age;
    
    // constructors, getters, setters
}
```

### Use the Template

```java
LadybugDBTemplate template = new LadybugDBTemplate(connectionFactory);

// Execute raw Cypher
template.execute("CREATE (p:Person {name: 'Alice', age: 30})");

// Query with mapping
List<Person> people = template.query(
    "MATCH (p:Person) RETURN p.name AS name, p.age AS age",
    (row) -> new Person(
        ValueMappers.asString(row.getValue("name")),
        ValueMappers.asInteger(row.getValue("age"))
    )
);
```

### Use the Repository

The repository requires `RowMapper` and `EntityWriter` to handle entity conversion.

```java
// Define mappers
RowMapper<Person> reader = (row) -> new Person(
    ValueMappers.asString(row.getValue("name")), 
    ValueMappers.asInteger(row.getValue("age"))
);

EntityWriter<Person> writer = (entity) -> Map.of(
    "age", entity.getAge() // ID (name) is handled automatically
);

// Create descriptors
EntityDescriptor<Person> personDescriptor = new EntityDescriptor<>(Person.class, reader, writer);
// define relationship descriptor similarly if needed, or pass null if not using relationships

SimpleNodeRepository<Person, Void, String> repository = new SimpleNodeRepository<>(
    template, 
    Person.class, 
    Void.class, 
    personDescriptor, 
    null // relationship descriptor
);

// CRUD operations
Person saved = repository.save(new Person("Bob", 25));
Optional<Person> found = repository.findById("Bob");
repository.deleteById("Bob");
```

### Parse Relationships

You can also map relationships, including their properties and connected nodes.

```java
// Define relationship entity
public class Follows {
    String id;
    Person from;
    Person to;
    int since;
    // constructors...
}

// Define mapper for relationship
RowMapper<Follows> followsMapper = (row) -> {
    // Get relationship data (properties, type, ids)
    RelationshipData rel = row.getRelationship("rel");
    String id = rel.id().toString(); // Internal ID
    int since = ValueMappers.asInteger(rel.properties().get("since"));

    // Get connected nodes (if returned by query)
    // Query: MATCH (s)-[rel:FOLLOWS]->(t) RETURN s, rel, t
    Map<String, Value> sourceProps = row.getNode("s");
    Map<String, Value> targetProps = row.getNode("t");

    Person from = new Person(
        ValueMappers.asString(sourceProps.get("name")), 
        ValueMappers.asInteger(sourceProps.get("age"))
    );
    
    Person to = new Person(
        ValueMappers.asString(targetProps.get("name")), 
        ValueMappers.asInteger(targetProps.get("age"))
    );

    return new Follows(id, from, to, since);
};
```

### use Custom Queries

You can execute custom Cypher queries using the `@Query` annotation on your repository interface.

```java
public interface PersonRepository extends NodeRepository<Person, String, Void, Person> {

    @Query("MATCH (p:Person) WHERE p.age > $minAge RETURN p")
    List<Person> findByAgeGreaterThan(@Param("minAge") int minAge);

    @Query("MATCH (p:Person {name: $name}) SET p.age = $newAge RETURN p")
    Optional<Person> updateAge(@Param("name") String name, @Param("newAge") int newAge);
}
```

## Components

| Component | Description |
|-----------|-------------|
| `LadybugDBTemplate` | Central class for executing Cypher queries |
| `SimpleNodeRepository` | Repository implementation for node entities |
| `LadybugDBTransactionManager` | Transaction manager (connection binding only, no commit/rollback) |
| `PooledConnectionFactory` | Connection pool using Apache Commons Pool2 |
| `SimpleConnectionFactory` | Simple connection factory (no pooling) |
| `rowMapper` / `QueryRow` | Interface for mapping query results to domain objects |

## Limitations

> [!CAUTION]
> **Single Writer Constraint**: LadybugDB only allows one write transaction at a time. Concurrent write operations will block waiting for the write lock, which can cause issues in multi-threaded applications.

### Transaction Behavior

Per LadybugDB documentation:
> "At any point in time, there can be multiple read transactions but only one write transaction"

**Implications:**
- The transaction manager provides **connection binding only** - it does not use explicit `BEGIN TRANSACTION`/`COMMIT`/`ROLLBACK`
- Each query **auto-commits immediately** 
- **Rollback is not supported** - once a command executes, it is committed
- Multiple read-only transactions can run in parallel without blocking

### Recommendations

1. **Keep write operations short** to minimize blocking time
2. **Use connection pooling** (`PooledConnectionFactory`) for efficient connection reuse
3. **Consider read-only transactions** for read-heavy workloads - they don't block writers

## Dependencies

```xml
<dependency>
    <groupId>com.ladybugdb</groupId>
    <artifactId>lbug</artifactId>
    <version>0.14.1</version>
</dependency>
<dependency>
    <groupId>org.neo4j</groupId>
    <artifactId>neo4j-cypher-dsl</artifactId>
    <version>2025.2.3</version>
</dependency>
```

## License

This project is part of [Archi-Knowledge](https://github.com/thecookiezen/Archi-Knowledge) and is licensed under the same terms.
