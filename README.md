# Archiledger

Archiledger combines Greek *arkhē* (origin, first principle) with "Ledger" - a foundational record serving as the source of truth for AI memory.

**Give your AI assistant a persistent memory and the power to build knowledge graphs.**

Archiledger is a specialized **Knowledge Graph** that serves as a **RAG (Retrieval-Augmented Generation)** system with **vector search**. It is exposed as a [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server to enable LLM-based assistants to store, connect, and recall information using a graph database. Whether you need a personal memory bank that persists across conversations or want to analyze codebases and documents into structured knowledge graphs, Archiledger provides the infrastructure to make your AI truly remember.

> **⚠️ Disclaimer:** This server implements **no authentication** and uses an **embedded graph database** designed for **local development only**. Not recommended for production.

## Why Archiledger?

LLMs are powerful, but they forget everything when a conversation ends:

- **Repeating yourself** — Telling your assistant the same preferences over and over
- **Lost insights** — Valuable analysis from one session isn't available in the next
- **No connected thinking** — Information lives in silos without relationships

Archiledger solves this with a **graph-based memory**:

| Problem | Solution |
|---------|----------|
| Context resets every conversation | Persistent notes that survive restarts |
| Flat, disconnected notes | Typed links between atomic notes (Zettelkasten) |
| No categorization | Tags and keywords on every note |
| No temporal awareness | ISO-8601 timestamps on every note |
| Keyword search limits | Vector search finds semantically similar notes |
| Hard to explore large graphs | Graph traversal via `LINKED_TO` relationships |

---

## Four Ways to Use Archiledger

```
┌────────────────────────────────────────────────────────────────────────────┐
│   LOW-LEVEL (Manual Control)              HIGH-LEVEL (AI-Powered)          │
│                                                                            │
│   ┌──────────────────┐                    ┌──────────────────┐             │
│   │  Core Module     │                    │ Agentic Memory   │             │
│   │  (Maven Dep)     │                    │ (Embabel)        │             │
│   │                  │                    │                  │             │
│   │ MemoryNoteService│                    │ • Agent          │             │
│   │ Direct Java API  │                    │ • RAG / Vector   │             │
│   └────────┬─────────┘                    │ • Auto-evolution │             │
│            │                              └────────┬─────────┘             │
│            ▼                                       ▼                       │
│   ┌──────────────────┐                    ┌──────────────────┐             │
│   │  MCP Server      │                    │ Agentic Memory   │             │
│   │  (LLM Tools)     │                    │ MCP              │             │
│   └──────────────────┘                    └──────────────────┘             │
│                                                                            │
│   No LLM Required ◄──────────────────────► LLM Required                    │
└────────────────────────────────────────────────────────────────────────────┘
```

### Quick Decision Guide

| Requirement | Recommended Approach |
|-------------|---------------------|
| Pure Java, no LLM | Core Module (Maven) |
| LLM with full manual control | MCP Server |
| AI classification in Java app | Agentic Memory (Embabel) |
| LLM with automatic memory management | Agentic Memory MCP |
| Full control over tags/links | Core Module or MCP Server |
| Automatic knowledge evolution | Agentic Memory (either) |

---

### 1. Core Module (Maven Dependency)

**Best for:** Java applications that need direct, programmatic control over memory operations without AI involvement.

```xml
<dependency>
    <groupId>com.thecookiezen</groupId>
    <artifactId>archiledger-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The `MemoryNoteService` interface provides full control over note creation, linking, similarity search, and graph traversal. No external LLM dependency required.

### 2. MCP Server (Low-Level Tools)

**Best for:** LLM-based assistants that need direct access to memory operations with full manual control.

The `mcp` module exposes all core operations as MCP tools. The LLM decides how to create notes, add tags, and establish links.

| Category | Tools |
|----------|-------|
| **Note Management** | `create_notes`, `get_note`, `get_notes_by_tag`, `delete_notes` |
| **Link Management** | `add_links`, `delete_links` |
| **Graph Exploration** | `read_graph`, `get_linked_notes`, `get_all_tags`, `search_notes` |

### 3. Agentic Memory (Embabel Module)

**Best for:** Java applications that want AI-powered memory management with automatic classification and evolution.

The `agentic-memory` module provides higher-level abstraction built on the [Embabel framework](https://github.com/embabel/embabel):

- **AgenticMemoryAgent**: Automatically analyzes content and suggests classifications
- **Vector Search**: Semantic similarity search across memory notes
- **Zoom Out Search**: Traverse upward in the knowledge graph to find related context
- **Memory Evolution**: AI evaluates whether new memories should link to existing ones
- **RAG Integration**: Built-in retrieval-augmented generation support

### 4. Agentic Memory MCP

**Best for:** LLM-based assistants that want AI-powered memory with minimal manual management.

The `agentic-memory-mcp` module exposes agentic memory capabilities as MCP tools. The AI handles classification, tagging, and linking automatically.

| Tool | Description |
|------|-------------|
| `memory_vector_search` | Perform semantic similarity search across memory notes |
| `memory_broaden_search` | Given a note ID, expand to find connected/linked notes |
| `memory_zoom_out` | Traverse upward in the knowledge graph to find parent/related notes |
| `agentic_memory_write` | Store content with automatic AI classification, tagging, and link generation |

---

## MCP Tools Reference

### Low-Level MCP Tools

#### Note Management

| Tool | Description |
|------|-------------|
| `create_notes` | Create one or more memory notes with content, keywords, tags, and optional links |
| `get_note` | Retrieve a specific note by ID (increments retrieval counter) |
| `get_notes_by_tag` | Find all notes with a given tag (e.g., `architecture`, `decision`, `bug`) |
| `delete_notes` | Delete notes by their IDs, including associated links and embeddings |

#### Link Management

| Tool | Description |
|------|-------------|
| `add_links` | Add typed links between notes with context (e.g., `DEPENDS_ON`, `RELATED_TO`, `CONTRADICTS`) |
| `delete_links` | Remove typed links between notes |

#### Graph Exploration

| Tool | Description |
|------|-------------|
| `read_graph` | Read the entire knowledge graph (all notes and links) |
| `get_linked_notes` | Find all notes directly connected to a given note |
| `get_all_tags` | List all unique tags currently used across notes |
| `search_notes` | Semantic similarity search with temperature scaling and threshold filtering |

### Agentic Memory MCP Tools

| Tool | Description |
|------|-------------|
| `memory_vector_search` | Semantic similarity search. Params: `query`, `topK` (default: 10), `threshold` (default: 0.5) |
| `memory_broaden_search` | Expand from a note to find connected notes. Params: `noteId`, `limit` (default: 10) |
| `memory_zoom_out` | Traverse upward in graph. Params: `noteId`, `limit` (default: 10) |
| `agentic_memory_write` | Store content with automatic classification. Params: `content` |

---

## Prerequisites

- Java 21 or higher
- Maven

## Building

```bash
mvn clean package
```

Builds all modules:
- `core/target/archiledger-core-*.jar` - Core library
- `mcp/target/archiledger-server-*.jar` - Low-level MCP server
- `agentic-memory/target/agentic-memory-*.jar` - Agentic memory library
- `agentic-memory-mcp/target/agentic-memory-mcp-*.jar` - Agentic memory MCP server

## Running

### Low-Level MCP Server

The server uses streamable HTTP transport on port **8080**.

**Transient (In-Memory):**
```bash
java -jar mcp/target/archiledger-server-1.0.0-SNAPSHOT.jar
```

**Persistent:**
```bash
java -Dladybugdb.data-path=./archiledger.lbdb \
     -jar mcp/target/archiledger-server-1.0.0-SNAPSHOT.jar
```

### Agentic Memory MCP Server

Requires LLM configuration for AI-powered features.

**Transient:**
```bash
java -jar agentic-memory-mcp/target/agentic-memory-mcp-1.0.0-SNAPSHOT.jar
```

**Persistent:**
```bash
java -Dladybugdb.data-path=./archiledger.lbdb \
     -jar agentic-memory-mcp/target/agentic-memory-mcp-1.0.0-SNAPSHOT.jar
```

### Running with Docker

**Transient (Data lost when container stops):**
```bash
docker run -p 8080:8080 registry.hub.docker.com/thecookiezen/archiledger:latest
```

**Persistent (Data saved to host filesystem):**
```bash
docker run -p 8080:8080 -v /path/to/local/data:/data registry.hub.docker.com/thecookiezen/archiledger:latest
```

**Custom data directory:**
```bash
docker run -p 8080:8080 \
  -e LADYBUGDB_DATA_PATH=/custom/data/archiledger.lbdb \
  -v /path/to/local/data:/custom/data \
  registry.hub.docker.com/thecookiezen/archiledger:latest
```

| Variable | Default | Description |
|----------|---------|-------------|
| `LADYBUGDB_DATA_PATH` | `/data/archiledger.lbdb` | File path where LadybugDB stores data |
| `LADYBUGDB_EXTENSION_DIR` | `/data/ladybugdb-extensions` | Directory for LadybugDB extension cache |

> **Note:** The `/data` volume must be writable by UID 1000 (`spring` user).

### Running Agentic Memory MCP with Docker

The agentic-memory-mcp server requires LLM configuration for AI-powered features.

**Transient (Data lost when container stops):**
```bash
docker run -p 8080:8080 \
  -e OPENAI_CUSTOM_BASE_URL=https://api.example.com \
  -e OPENAI_CUSTOM_MODELS=model-name \
  -e OPENAI_CUSTOM_API_KEY=your_api_key \
  registry.hub.docker.com/thecookiezen/archiledger-agentic-memory:latest
```

**Persistent (Data saved to host filesystem):**
```bash
docker run -p 8080:8080 \
  -v /path/to/local/data:/data \
  -e OPENAI_CUSTOM_BASE_URL=https://api.example.com \
  -e OPENAI_CUSTOM_MODELS=model-name \
  -e OPENAI_CUSTOM_API_KEY=your_api_key \
  registry.hub.docker.com/thecookiezen/archiledger-agentic-memory:latest
```

#### LLM Configuration Environment Variables

| Variable | Description |
|----------|-------------|
| `OPENAI_CUSTOM_BASE_URL` | Base URL for the OpenAI-compatible API |
| `OPENAI_CUSTOM_MODELS` | Model name to use |
| `OPENAI_CUSTOM_API_KEY` | API key for authentication |
| `OPENAI_CUSTOM_COMPLETIONS_PATH` | Optional: Custom completions endpoint path (default: `/v1/chat/completions`) |

#### Agentic Memory Docker Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `LADYBUGDB_DATA_PATH` | `/data/archiledger.lbdb` | File path where LadybugDB stores data |
| `LADYBUGDB_EXTENSION_DIR` | `/data/ladybugdb-extensions` | Directory for LadybugDB extension cache |
| `INITIAL_MEMORY` | `256m` | JVM initial heap size |
| `MAX_MEMORY` | `512m` | JVM maximum heap size |
| `MAX_RAM_PERCENTAGE` | `75.0` | JVM max RAM percentage |

> **Note:** The `/data` volume must be writable by UID 1000 (`spring` user).

### Visualizing the Graph

Use [Ladybug BugScope](https://github.com/LadybugDB/bugscope) to visualize your graph:
1. Open BugScope and connect using the Ladybug data directory URI
2. Run Cypher queries like `MATCH (n) RETURN n` to explore your knowledge graph

---

## Configuration

### Server Properties

```properties
spring.ai.mcp.server.name=archiledger-server
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.protocol=STREAMABLE
server.port=8080
```

### CORS Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `cors.enabled` | `false` | Enable CORS support |
| `cors.allow-any-origin` | `false` | Set `Access-Control-Allow-Origin` to `*` |
| `cors.origins` | `[]` | Explicit list of permitted origins |
| `cors.match-origins` | `[]` | Regex patterns for dynamic origin matching |
| `cors.allow-credentials` | `false` | Add `Access-Control-Allow-Credentials` header |
| `cors.max-age` | `7200` | Preflight cache duration in seconds |

**Development (Permissive):**
```properties
cors.enabled=true
cors.allow-any-origin=true
```

**Production (Restricted):**
```properties
cors.enabled=true
cors.origins=https://my-secure-frontend.internal
cors.allow-credentials=true
```

**Dynamic Subdomains:**
```properties
cors.enabled=true
cors.match-origins=^http://localhost:\\d+$,^https://.*\\.my-company\\.com$
```

> [!IMPORTANT]
> For credentialed requests, use explicit origins or regex patterns. `cors.allow-any-origin` will be rejected by browsers for credentialed requests.

### Vector Storage

| Property | Default | Description |
|----------|---------|-------------|
| `ladybugdb.extension-dir` | `~/.lbug/extensions` | LadybugDB extension cache directory |

Embeddings are stored using LadybugDB's native vector extension with HNSW indexing.

### HNSW Index Configuration

Tune the HNSW (Hierarchical Navigable Small World) index parameters for optimal performance:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `ladybugdb.hnsw.mu` | `24` | Max degree Upper - lower = faster search, less memory |
| `ladybugdb.hnsw.ml` | `48` | Max degree Lower - higher = better recall |
| `ladybugdb.hnsw.pu` | `0.1` | Sampling rate for upper graph (10% = 1000 nodes from 10k) |
| `ladybugdb.hnsw.efc` | `300` | Construction effort - higher = better index quality, slower indexing |
| `ladybugdb.hnsw.metric` | `cosine` | Distance metric (`cosine`, `euclidean`, `dot_product`) |

**Resource Estimates (10k records, 384-dim vectors):**

| Resource | Estimate |
|----------|----------|
| Vector Storage | ~30.7 MB |
| Index Overhead | ~3.8 MB |
| Total RAM | ~35 MB |

### Embedding Model Configuration

By default, Archiledger uses a local ONNX model (`all-MiniLM-L6-v2`, 384 dimensions) that requires no external API. You can customize the embedding model using environment variables.

#### Model Comparison

| Model | Dimensions | Quality (MTEB) | Speed | Best For |
|-------|------------|----------------|-------|----------|
| all-MiniLM-L6-v2 | 384 | ~57.8 | Fastest | Development, quick prototyping |
| bge-small-en-v1.5 | 384 | ~62.0 | Fast | Production, better quality at same size |
| all-mpnet-base-v2 | 768 | ~63.5 | Medium | Higher accuracy, nuanced semantics |
| bge-large-en-v1.5 | 1024 | ~64.2 | Slowest | Maximum accuracy, cross-domain |

#### Option 1: Custom HuggingFace ONNX Models

Use any ONNX-compatible model from HuggingFace:

```bash
export SPRING_AI_EMBEDDING_TRANSFORMER_ONNX_MODELURI=https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/onnx/model.onnx
export SPRING_AI_EMBEDDING_TRANSFORMER_TOKENIZER_URI=https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/tokenizer.json
export LADYBUGDB_EMBEDDING_DIMENSIONS=384

java -jar mcp/target/archiledger-server-1.0.0-SNAPSHOT.jar
```

#### Option 2: OpenAI-Compatible APIs (OpenAI, ZhiPu AI, Mistral, etc.)

```bash
# OpenAI
export SPRING_AI_OPENAI_BASE_URL=https://api.openai.com
export SPRING_AI_OPENAI_API_KEY=sk-your-api-key
export SPRING_AI_OPENAI_EMBEDDING_OPTIONS_MODEL=text-embedding-3-small
export LADYBUGDB_EMBEDDING_DIMENSIONS=1536

# ZhiPu AI
export SPRING_AI_OPENAI_BASE_URL=https://open.bigmodel.cn/api/paas/v4
export SPRING_AI_OPENAI_API_KEY=your-zhipu-api-key
export SPRING_AI_OPENAI_EMBEDDING_OPTIONS_MODEL=embedding-3
export LADYBUGDB_EMBEDDING_DIMENSIONS=2048

java -jar mcp/target/archiledger-server-1.0.0-SNAPSHOT.jar
```

#### Option 3: Ollama Local Models

```bash
# Ensure Ollama is running: ollama pull nomic-embed-text
export SPRING_AI_OPENAI_BASE_URL=http://localhost:11434
export SPRING_AI_OPENAI_EMBEDDING_OPTIONS_MODEL=nomic-embed-text
export LADYBUGDB_EMBEDDING_DIMENSIONS=768

java -jar mcp/target/archiledger-server-1.0.0-SNAPSHOT.jar
```

#### Docker with Custom Embeddings

```bash
# Ollama
docker run -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e SPRING_AI_OPENAI_BASE_URL=http://host.docker.internal:11434 \
  -e SPRING_AI_OPENAI_EMBEDDING_OPTIONS_MODEL=nomic-embed-text \
  -e LADYBUGDB_EMBEDDING_DIMENSIONS=768 \
  registry.hub.docker.com/thecookiezen/archiledger:latest
```

#### Embedding Environment Variables

| Variable | Description |
|----------|-------------|
| `SPRING_AI_EMBEDDING_TRANSFORMER_ONNX_MODELURI` | HuggingFace ONNX model URL |
| `SPRING_AI_EMBEDDING_TRANSFORMER_TOKENIZER_URI` | HuggingFace tokenizer JSON URL |
| `SPRING_AI_OPENAI_BASE_URL` | OpenAI-compatible API base URL |
| `SPRING_AI_OPENAI_API_KEY` | API key for authentication |
| `SPRING_AI_OPENAI_EMBEDDING_OPTIONS_MODEL` | Embedding model name |
| `LADYBUGDB_EMBEDDING_DIMENSIONS` | Vector dimensions (must match model, default: 384) |

> **Important:** When changing embedding models, the dimensions must match your model's output. Common dimensions: all-MiniLM-L6-v2 (384), nomic-embed-text (768), text-embedding-3-small (1536).

---

## MCP Client Connection

Connect via: **Streamable HTTP Endpoint**: `http://localhost:8080/mcp`

### Client Configuration Examples

**Gemini CLI (`settings.json`):**
```json
{
  "mcpServers": {
    "archiledger": {
      "httpUrl": "http://localhost:8080/mcp"
    }
  }
}
```

**VSCode / GitHub Copilot (`settings.json`):**
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

**Antigravity:**
```json
{
  "mcpServers": {
    "archiledger": {
      "serverUrl": "http://localhost:8080/mcp"
    }
  }
}
```

### Docker Tips for MCP Clients

1. **Persistent Data**: Always mount a volume (`-v`) to preserve your knowledge graph
2. **Container Lifecycle**: Run with `-d` (detached mode)
3. **Port Conflicts**: Map to different port (e.g., `-p 9090:8080`) and update URL
4. **Named Containers**: Use `--name archiledger` for easy management
5. **Debug Logs**: `docker logs archiledger`

---

## Usage Examples

### Use Case 1: Memory Bank

Use the knowledge graph as a persistent memory bank. The LLM stores atomic pieces of knowledge as notes, tags them, and links related notes.

```markdown
# Memory Bank Instructions

You have access to a knowledge graph MCP server. Use it to store and retrieve atomic notes across conversations.

## Core Behaviors

### Proactive Memory Storage
When the user shares important information, store it as an atomic note:
- **Preferences**: User's coding style, preferred tools, naming conventions
- **Decisions**: Architecture decisions, technology choices, rejected alternatives
- **Context**: Project goals, constraints, team information
- **Tasks**: Ongoing work, blockers, next steps

### Tagging Notes
Use tags for categorization:
- `preference` - User preferences and settings
- `decision` - Important decisions with rationale
- `context` - Project or domain context
- `task` - Work items and their status
- `observation` - General notes and observations
- `person` - Team members and stakeholders

### Creating Notes
1. Give the note a descriptive ID (e.g., `java-naming-convention`)
2. Write focused content (one idea per note — Zettelkasten atomicity)
3. Add relevant keywords for search
4. Set appropriate tags
5. Link to related notes with context

### Recalling Notes
At the start of each conversation:
1. Use `read_graph` to get an overview
2. Use `search_notes` to find semantically relevant notes
3. Use `get_notes_by_tag` to retrieve by category
4. Reference stored decisions and preferences in responses

### Linking Notes
Use typed links with context:
- `RELATES_TO` - General relationship
- `DEPENDS_ON` - Dependency relationship
- `AFFECTS` - One thing impacts another
- `PART_OF` - Component/container relationship
- `SUPERSEDES` - Replaces previous decision/approach
- `CONTRADICTS` - Conflicts with another note

> **Note:** Each link requires a `context` field explaining why the relationship exists.
```

---

### Use Case 2: Codebase/Document Analysis

Build a structured knowledge base from a codebase or document corpus.

```markdown
# Codebase Knowledge Graph Builder

Use the memory MCP server to create atomic knowledge notes from the codebase.

## Analysis Workflow

### Phase 1: High-Level Structure
1. Identify major modules, packages, or services
2. Create a note for each architectural component
3. Link notes with `DEPENDS_ON`, `CONTAINS`, or `USES` links

### Phase 2: Deep Dive
For each component:
1. Key classes, interfaces, and their responsibilities
2. Important functions and their purposes
3. Data models and their relationships
4. External integrations and APIs

### Phase 3: Cross-Cutting Concerns
1. Design patterns in use
2. Shared utilities and helpers
3. Configuration and environment handling
4. Error handling strategies

## Tags for Code Analysis
- `module` - Top-level packages, services, or bounded contexts
- `component` - Major classes, interfaces, or subsystems
- `function` - Important functions or methods
- `model` - Data models, DTOs, entities
- `pattern` - Design patterns in use
- `config` - Configuration classes or files
- `api` - External or internal API endpoints
- `dependency` - External libraries or services

## Link Types for Code
- `DEPENDS_ON` - Class/module depends on another
- `IMPLEMENTS` - Implements an interface or contract
- `EXTENDS` - Inherits from another class
- `USES` - Utilizes another component
- `CALLS` - Function calls another function
- `CONTAINS` - Package contains class, class contains method
- `PRODUCES` - Creates or emits events/messages
- `CONSUMES` - Handles events/messages

## Querying for Investigation
1. **Find dependencies**: Get a note and examine its links
2. **Impact analysis**: Follow `DEPENDS_ON` links to find affected components
3. **Understand data flow**: Trace `CALLS`, `PRODUCES`, `CONSUMES` links
4. **Onboarding**: Search by `module` tag, then explore linked `component` notes

## Best Practices
1. **One idea per note** — Zettelkasten atomicity
2. **Include file paths** in content or keywords
3. **Document "why"** not just "what"
4. **Update incrementally** as you explore
5. **Link with context** — explanatory context makes the graph valuable
```

---

## Architecture

- **Domain Layer**: Core domain model (`MemoryNote`, `MemoryNoteId`, `NoteLink`). Defines the repository port (`MemoryNoteRepository`).
- **Application Layer**: Orchestrates domain logic using `MemoryNoteService`. Handles retrieval count tracking and embedding generation.
- **Infrastructure Layer**:
  - **Persistence**: `LadybugMemoryNoteRepository` - LadybugDB graph database. Notes stored as nodes, links as `LINKED_TO` relationships.
  - **Vector Search**: `LadybugEmbeddingsService` uses LadybugDB's native vector extension with HNSW indexing.
  - **MCP**: Exposes memory tools via `McpToolAdapter`.

### Agentic Memory Module

The `agentic-memory` module provides AI-driven memory evolution:

- **AgenticMemoryAgent**: Analyzes notes and suggests new links based on semantic relationships
- **Context-Aware Links**: Automatically evaluates whether to add, update, or remove links
- **Evolution Prompts**: Uses Jinja templates for content analysis and evolution evaluation
- **MemoryNoteSearchOperations**: Implements RAG interfaces for vector search and result expansion

---

## Limitations & Performance

> **⚠️ Important:** Designed for local development, personal use, and small-to-medium datasets.

| Limitation | Impact | Mitigation |
|------------|--------|------------|
| **Embedded LadybugDB** | Single-process, limited concurrency | Suitable for <100k notes |
| **No authentication** | All operations unauthenticated | Local/trusted environments only |
| **Heap-limited** | Large `read_graph` may OOM | Increase heap (`-Xmx`) or paginate |

### Performance (512MB heap)

| Operation | Throughput | Notes |
|-----------|------------|-------|
| Note creation | ~50-100 ops/sec | Using Cypher inserts |
| Link creation | ~30-60 ops/sec | Depends on graph connectivity |
| Note lookup by ID | <10ms | Direct index lookup |
| Similarity search | O(n) | Scales linearly with note count |

> **💡 Tip:** For load testing see [LOAD_TESTING.md](./LOAD_TESTING.md).
