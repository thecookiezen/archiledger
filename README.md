# Archiledge (Memory MCP Server)

Graph based implementation of the Model Context Protocol (MCP) Memory Server.

> **⚠️ Disclaimer:** This server currently implements **no authentication** mechanisms. Additionally, it relies on an **embedded graph database** (or in-memory storage) which is designed and optimized for **local development and testing environments only**. It is **not recommended for production use** in its current state.

## Features

- **Knowledge Graph**: Stores entities and relations.
- **MCP Tools**:
  - `create_entities`: Create new entities.
  - `create_relations`: Create relations between entities.
  - `read_graph`: Read the entire graph.
  - `search_nodes`: Search for entities.
  - `delete_entities`: Delete entities by name.
  - `delete_relations`: Delete relations.

## Architecture

- **Domain Layer**: Contains the core business logic and entities (`Entity`, `Relation`). It defines the repository interface (`KnowledgeGraphRepository`).
- **Application Layer**: Orchestrates the domain logic using services (`KnowledgeGraphService`).
- **Infrastructure Layer**:
  - **Persistence**: 
    - `InMemoryKnowledgeGraphRepository`: In-memory implementation (default).
    - `Neo4jKnowledgeGraphRepositoryAdapter`: Neo4j implementation (activates with `neo4j` profile).
  - **MCP**: Acts as the primary adapter, exposing tools via the `McpToolAdapter`.

## Prerequisites

- Java 21 or higher
- Maven
- Neo4j Database (if using `neo4j` profile without embedded setup)

## Building

```bash
mvn clean package
```

## Running

The server uses streamable HTTP transport by default on port **8080**.

### Default (In-Memory)
```bash
java -jar mcp/target/archiledge-server-0.0.1-SNAPSHOT.jar
```

### With Neo4j (Embedded)
This mode runs a Neo4j server inside the application process.

**Transient (Data lost on restart):**
```bash
java -Dspring.profiles.active=neo4j -Dspring.neo4j.uri=embedded -jar mcp/target/archiledge-server-0.0.1-SNAPSHOT.jar
```

**Persistent (Data saved to file):**
Set the `memory.neo4j.data-dir` property to a directory path.
```bash
java -Dspring.profiles.active=neo4j \
     -Dspring.neo4j.uri=embedded \
     -Dmemory.neo4j.data-dir=./neo4j-data \
     -jar mcp/target/archiledge-server-0.0.1-SNAPSHOT.jar
```

### With Neo4j (External)
Configure Neo4j connection details in `application.properties` or via environment variables, then run with the `neo4j` profile.

```bash
export SPRING_NEO4J_URI=bolt://localhost:7687
export SPRING_NEO4J_AUTHENTICATION_USERNAME=neo4j
export SPRING_NEO4J_AUTHENTICATION_PASSWORD=secret
java -Dspring.profiles.active=neo4j -jar mcp/target/archiledge-server-0.0.1-SNAPSHOT.jar
```

## Configuration

Configuration is located in `src/main/resources/application.properties`.

```properties
spring.ai.mcp.server.name=archiledge-server
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.protocol=STREAMABLE
server.port=8080
```

## MCP Client Connection

Once the server is running, MCP clients can connect via:
- **Streamable HTTP Endpoint**: `http://localhost:8080/mcp`