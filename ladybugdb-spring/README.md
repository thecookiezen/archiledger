# LadybugDB Spring

A Spring Data-like integration framework for [LadybugDB](https://ladybugdb.com), providing familiar Spring patterns for graph database operations.

## Features

- **Repository Pattern**: Spring Data-style `NodeRepository` for CRUD operations on graph nodes
- **Template Support**: `LadybugDBTemplate` for executing Cypher queries with connection management
- **Connection Pooling**: Built-in connection pooling via `PooledConnectionFactory`
- **Cypher DSL Integration**: Use [Neo4j Cypher DSL](https://github.com/neo4j/cypher-dsl) for type-safe query building
- **Entity Mapping**: Annotation-based entity mapping with `@NodeEntity` and `@Id`

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
    "MATCH (p:Person) RETURN p.name, p.age",
    (row, rowNum) -> new Person(
        ValueMappers.asString(row.getValue(0)),
        ValueMappers.asInteger(row.getValue(1))
    )
);
```

### Use the Repository

```java
SimpleNodeRepository<Person, String> repository = new SimpleNodeRepository<>(
    template, Person.class, entityDescriptor
);

// CRUD operations
Person saved = repository.save(new Person("Bob", 25));
Optional<Person> found = repository.findById("Bob");
repository.deleteById("Bob");
```

## Components

| Component | Description |
|-----------|-------------|
| `LadybugDBTemplate` | Central class for executing Cypher queries |
| `SimpleNodeRepository` | Repository implementation for node entities |
| `LadybugDBTransactionManager` | Spring transaction manager (connection binding) |
| `PooledConnectionFactory` | Connection pool using Apache Commons Pool2 |
| `SimpleConnectionFactory` | Simple connection factory (no pooling) |

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
