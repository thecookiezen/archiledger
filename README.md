# Archiledger

**Give your AI assistant a persistent memory and the power to build knowledge graphs.**

Archiledger is a [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that enables LLM-based assistants to store, connect, and recall information using a graph database. Whether you need a personal memory bank that persists across conversations or want to analyze codebases and documents into structured knowledge graphs, Archiledger provides the infrastructure to make your AI truly remember.

> **⚠️ Disclaimer:** This server currently implements **no authentication** mechanisms. Additionally, it relies on an **embedded graph database** (or in-memory storage) which is designed and optimized for **local development and testing environments only**. It is **not recommended for production use** in its current state.

## Why Archiledger?

LLMs are powerful, but they forget everything the moment a conversation ends. This creates frustrating experiences:

- **Repeating yourself** — Telling your assistant the same preferences, project context, or decisions over and over
- **Lost insights** — Valuable analysis from one session isn't available in the next
- **No connected thinking** — Information lives in silos without relationships between concepts

Archiledger solves this by giving your AI a **graph-based memory**:

| Problem | Archiledger Solution |
|---------|---------------------|
| Context resets every conversation | Persistent storage that survives restarts |
| Flat, disconnected notes | Graph structure with entities and relations |
| Manual note-taking | AI automatically stores and retrieves relevant info |
| Hard to explore large codebases | Build navigable knowledge graphs from code |
| Investigation dead ends | Follow relationships to discover connections |

The graph model is particularly powerful because knowledge isn't flat — concepts relate to each other. When your AI can traverse these connections, it can provide richer context and discover non-obvious relationships.

## Features

- **Knowledge Graph**: Stores entities and relations.
- **MCP Tools**:
  - **Entity Management**:
    - `create_entities`: Create new entities in the knowledge graph. Entities are nodes representing things like people, places, concepts, etc.
    - `get_entity`: Get a specific entity by its name. Returns the entity with its type and observations.
    - `get_entities_by_type`: Get all entities of a specific type (e.g., Person, Component, Service).
    - `delete_entities`: Delete entities from the knowledge graph by their names.
  - **Relation Management**:
    - `create_relations`: Create relations between entities. Relations are edges representing how entities are connected.
    - `get_relations_for_entity`: Get all relations (incoming and outgoing) for a specific entity.
    - `get_relations_by_type`: Get all relations of a specific type (e.g., DEPENDS_ON, USES, CONTAINS).
    - `delete_relations`: Delete relations from the knowledge graph.
  - **Graph Exploration**:
    - `read_graph`: Read the entire knowledge graph. Returns all entities and relations.
    - `get_related_entities`: Find all entities directly connected to a given entity.
    - `get_entity_types`: List all unique entity types in the graph.
    - `get_relation_types`: List all unique relation types in the graph.

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

## Building

```bash
mvn clean package
```

## Running

The server uses streamable HTTP transport by default on port **8080**.

### Default (In-Memory)
```bash
java -jar mcp/target/archiledger-server-0.0.1-SNAPSHOT.jar
```

### With Neo4j (Embedded)
This mode runs a Neo4j server inside the application process.

**Transient (Data lost on restart):**
```bash
java -Dspring.profiles.active=neo4j -Dspring.neo4j.uri=embedded -jar mcp/target/archiledger-server-0.0.1-SNAPSHOT.jar
```

**Persistent (Data saved to file):**
Set the `memory.neo4j.data-dir` property to a directory path.
```bash
java -Dspring.profiles.active=neo4j \
     -Dspring.neo4j.uri=embedded \
     -Dmemory.neo4j.data-dir=./neo4j-data \
     -jar mcp/target/archiledger-server-0.0.1-SNAPSHOT.jar
```

> **💡 Tip: Viewing the Graph with Neo4j Browser**
>
> When using embedded Neo4j, you can visualize your graph using [Neo4j Browser](https://github.com/neo4j/neo4j-browser). The embedded database exposes a Bolt endpoint on a dynamic port:
> 1. **Keep** the Archiledger server running.
> 2. Check the server logs for the Bolt URI, e.g.: `Driver instance ... created for server uri 'bolt://localhost:35157'`
> 3. Open Neo4j Browser (default: http://localhost:8080) and connect using the Bolt URI from the logs.
> 4. Run Cypher queries like `MATCH (n) RETURN n` to explore your knowledge graph.

### Running with Docker

The Docker image supports configurable data persistence and Neo4j port configuration.

**Transient (Data lost when container stops):**
```bash
docker run -p 8080:8080 registry.hub.docker.com/thecookiezen/archiledger:latest
```

**Persistent (Data saved to host filesystem):**
Mount a local directory to `/data/neo4j` inside the container:
```bash
docker run -p 8080:8080 -v /path/to/local/neo4j-data:/data/neo4j registry.hub.docker.com/thecookiezen/archiledger:latest
```

**With Neo4j Bolt port exposed (for Neo4j Browser access):**
Expose the Bolt port to connect with external tools like Neo4j Browser:
```bash
docker run -p 8080:8080 -p 7687:7687 \
  -v /path/to/local/neo4j-data:/data/neo4j \
  registry.hub.docker.com/thecookiezen/archiledger:latest
```

**Custom Bolt port:**
Override the default Bolt port (7687) using the `NEO4J_BOLT_PORT` environment variable:
```bash
docker run -p 8080:8080 -p 17687:17687 \
  -e NEO4J_BOLT_PORT=17687 \
  -v /path/to/local/neo4j-data:/data/neo4j \
  registry.hub.docker.com/thecookiezen/archiledger:latest
```

**Custom data directory (Optional):**
Override the default data directory path using the `NEO4J_DATA_DIR` environment variable:
```bash
docker run -p 8080:8080 -p 7687:7687 \
  -e NEO4J_DATA_DIR=/custom/data/path \
  -v /path/to/local/data:/custom/data/path \
  registry.hub.docker.com/thecookiezen/archiledger:latest
```

#### Docker Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `NEO4J_DATA_DIR` | `/data/neo4j` | Directory where Neo4j stores its data |
| `NEO4J_BOLT_PORT` | `7687` | Port for Neo4j Bolt connections |

> **💡 Note:** The data directory at `/data/neo4j` (or your custom path) must be writable by the container user (UID 100, `spring` user). If you encounter permission errors, ensure your host directory has appropriate permissions:
> ```bash
> mkdir -p /path/to/local/neo4j-data
> chmod 777 /path/to/local/neo4j-data  # or chown to UID 100
> ```

## Configuration

Configuration is located in `src/main/resources/application.properties`.

```properties
spring.ai.mcp.server.name=archiledger-server
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.protocol=STREAMABLE
server.port=8080
```

## MCP Client Connection

Once the server is running, MCP clients can connect via:
- **Streamable HTTP Endpoint**: `http://localhost:8080/mcp`

---

## Usage with LLM

This MCP server can be used with LLM-based assistants (like GitHub Copilot, Gemini CLI, or other MCP-compatible clients) for various knowledge management scenarios. Below are two primary use cases with example instructions.

### Use Case 1: Memory Bank

Use the knowledge graph as a persistent memory bank to store and recall information across conversations. The LLM can remember context, preferences, project notes, and important decisions.

```markdown
# Memory Bank Instructions

You have access to a knowledge graph MCP server that serves as your persistent memory. Use it to store and retrieve important information across our conversations.

## Core Behaviors

### Proactive Memory Storage
When the user shares important information, store it automatically:
- **Preferences**: User's coding style, preferred tools, naming conventions
- **Decisions**: Architecture decisions, technology choices, rejected alternatives
- **Context**: Project goals, constraints, team information
- **Tasks**: Ongoing work, blockers, next steps

### Memory Structure
Use these entity types for organization:
- `preference` - User preferences and settings
- `decision` - Important decisions with rationale
- `context` - Project or domain context
- `task` - Work items and their status
- `note` - General notes and observations
- `person` - Team members and stakeholders

### Creating Memories
When storing information:
1. Create an entity with a descriptive name
2. Set the appropriate entityType
3. Add detailed observations (store reasoning, not just facts)

### Recalling Memories
At the start of each conversation:
1. Use `read_graph` to get an overview of stored knowledge
2. Use `search_nodes` to find relevant context for the current task
3. Reference stored decisions and preferences in your responses

### Creating Relations
Link related memories for better context.

#### Relation Types
- `RELATES_TO` - General relationship
- `DEPENDS_ON` - Dependency relationship
- `AFFECTS` - One thing impacts another
- `PART_OF` - Component/container relationship
- `SUPERSEDES` - Replaces previous decision/approach
```

---

### Use Case 2: Codebase/Document Analysis

Use the knowledge graph to build a structured representation of a codebase or document corpus. This is valuable for onboarding, architecture documentation, investigation, and understanding complex systems.

```markdown
# Codebase Knowledge Graph Builder

You have access to a knowledge graph MCP server. Use it to create a structured knowledge base of the codebase for architecture documentation, onboarding, and investigation.

## Analysis Workflow

### Phase 1: High-Level Structure
Start by mapping the overall architecture:
1. Identify major modules, packages, or services
2. Create entities for each architectural component
3. Map dependencies between components

### Phase 2: Deep Dive
For each component, analyze and document:
1. Key classes, interfaces, and their responsibilities
2. Important functions and their purposes
3. Data models and their relationships
4. External integrations and APIs

### Phase 3: Cross-Cutting Concerns
Document patterns that span multiple components:
1. Design patterns in use
2. Shared utilities and helpers
3. Configuration and environment handling
4. Error handling strategies

## Entity Types for Code Analysis

Use these entity types:
- `module` - Top-level packages, services, or bounded contexts
- `component` - Major classes, interfaces, or subsystems
- `function` - Important functions or methods
- `model` - Data models, DTOs, entities
- `pattern` - Design patterns in use
- `config` - Configuration classes or files
- `api` - External or internal API endpoints
- `dependency` - External libraries or services

## Creating Code Entities

When analyzing code, create detailed entities.

## Relation Types for Code

Use these relation types:
- `DEPENDS_ON` - Class/module depends on another
- `IMPLEMENTS` - Implements an interface or contract
- `EXTENDS` - Inherits from another class
- `USES` - Utilizes another component
- `CALLS` - Function calls another function
- `CONTAINS` - Package contains class, class contains method
- `PRODUCES` - Creates or emits events/messages
- `CONSUMES` - Handles events/messages

## Querying for Investigation

Use the graph for code investigation:

1. **Find dependencies**: Search for a component and examine its relations
2. **Impact analysis**: Follow `DEPENDS_ON` relations to find affected components
3. **Understand data flow**: Trace `CALLS`, `PRODUCES`, `CONSUMES` relations
4. **Onboarding**: Start with `module` entities, then drill into `component` entities

## Best Practices

1. **Be consistent** with naming (use class names, not descriptions)
2. **Include file paths** in observations for easy navigation
3. **Document "why"** not just "what" - capture design rationale
4. **Update incrementally** - add to the graph as you explore
5. **Link generously** - relations are what make the graph valuable
```

---

### MCP Server Configuration for LLM Clients

Configure your LLM client to connect to the Archiledger MCP server. Below are examples for common clients.

#### Gemini CLI (`settings.json`)

```json
{
  "mcpServers": {
    "archiledger": {
      "httpUrl": "http://localhost:8080/mcp"
    }
  }
}
```

#### VSCode / GitHub Copilot (`settings.json`)

```json
{
  "servers": {
    "archiledger": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

#### Antigravity

```json
{
  "mcpServers": {
      "archiledger": {
          "serverUrl": "http://localhost:8080/mcp"
      }
  }
}
```

### Docker Container Tips for MCP Clients

1. **Persistent Data**: Always mount a volume (`-v`) to preserve your knowledge graph across container restarts.

2. **Container Lifecycle**: Run the container separately with `-d` (detached mode).

3. **Port Conflicts**: If port 8080 is in use, map to a different host port (e.g., `-p 9090:8080`) and update the URL accordingly.

4. **Named Containers**: Use `--name archiledger` to easily manage the container:
   ```bash
   docker stop archiledger && docker rm archiledger
   ```

5. **Check Container Logs**: Debug connection issues with:
   ```bash
   docker logs archiledger
   ```
