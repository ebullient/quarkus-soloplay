# Contributing to Soloplay

Thanks for your interest in contributing! This guide covers the technical architecture, development setup, and coding patterns you'll need to work on Soloplay.

## Project Overview

Soloplay is an AI-powered assistant for solo tabletop RPG gameplay that combines:
- Story management (characters, locations, events, relationships) in a graph database
- RAG (Retrieval Augmented Generation) queries against campaign documents
- AI tools that can autonomously manage story elements
- Local LLM inference (no cloud dependencies)

**Tech Stack:**

- **Java 21** - Modern Java features (records, pattern matching, etc.)
- **Quarkus 3.30.2** - Supersonic Subatomic Java framework with Quarkus REST
- **LangChain4j** - AI service orchestration with declarative interfaces
- **Ollama** - Local LLM inference (mistral-nemo:12b for chat, nomic-embed-text for embeddings)
- **Neo4j** - Graph database for story data and vector embeddings (768 dimensions)
- **Renarde MVC** - Type-safe server-side rendering
- **Qute** - Templating engine
- **CommonMark** - Markdown to HTML conversion

## Build Commands

### Development

```bash
# Run in dev mode with live reload
./mvnw quarkus:dev

# Dev UI available at http://localhost:8080/q/dev/
```

### Testing

```bash
# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify

# Run a single test class
./mvnw test -Dtest=ClassName
```

### Building

```bash
# Standard package
./mvnw package

# Build uber-jar
./mvnw package -Dquarkus.package.jar.type=uber-jar

# Native executable (requires GraalVM)
./mvnw package -Dnative

# Native build in container
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## Local Development Setup

### Prerequisites

You'll need the following running locally:

1. **Ollama** with required models:

   ```bash
   # Install Ollama from https://ollama.ai

   # Pull required models
   ollama pull mistral-nemo:12b
   ollama pull nomic-embed-text
   ```

2. **Neo4j** database:

   ```bash
   # Using Docker
   docker run -d \
     --name neo4j \
     -p 7474:7474 -p 7687:7687 \
     -e NEO4J_AUTH=neo4j/password \
     neo4j:latest
   ```

### Configuration

The application expects these services at default locations:

- Ollama: Configure via `quarkus.langchain4j.ollama.base-url` in application.properties
- Neo4j: Configure via standard Quarkus Neo4j properties

See [src/main/resources/application.properties](src/main/resources/application.properties) for configuration options.

## Architecture Overview

### AI Services Pattern

The application uses LangChain4j's declarative AI service interfaces:

- **ChatAssistant**: Simple chat interface - methods are auto-implemented by LangChain4j
- **SettingAssistant**: RAG-enabled queries - automatically retrieves relevant embeddings

These are plain Java interfaces annotated with `@RegisterAiService`. Quarkus LangChain4j generates the implementation at build time.

### Document Processing Pipeline

**IngestService** handles document ingestion:

1. **Upload**: Accepts markdown files via multipart form data (through web UI at `/ingest`)
2. **Parse**: Handles two formats:
   - Structured markdown with YAML frontmatter and section headers
   - Plain text with recursive chunking
3. **Embed**: Generates embeddings using Ollama's nomic-embed-text
4. **Store**: Saves text segments + embeddings in Neo4j with metadata

### REST API

The API is organized into three main areas:

- **ChatResource** (`/api/chat`) - Generic LLM chat (setting-independent)
    - `GET/POST /api/chat` - Direct LLM chat (no retrieval)

- **LoreResource** (`/api/lore`) - RAG queries against ingested documents
    - `GET/POST /api/lore` - RAG queries with embedding retrieval

- **StoryResource** (`/api/story`) - Story-specific operations
    - `GET /api/story/list` - List story thread IDs

All responses are converted from markdown to HTML.

### Data Flow

```
User Request → REST Endpoint → AI Service Interface → LangChain4j
                                                     ↓
                                        [RAG: Query Neo4j Embeddings]
                                                     ↓
                                              Ollama LLM
                                                     ↓
                                    Response → Markdown → HTML
```

## Code Standards

- **Java 21**: Use modern Java features (records, pattern matching, etc.)
- **Quarkus REST**: Use `quarkus-rest` (not `quarkus-resteasy`)
    - Annotations: `@RestQuery`, `@RestForm` from `org.jboss.resteasy.reactive`
- **Dependency Injection**: Use `@Inject` and `@ApplicationScoped`
- **AI Services**: Declarative interfaces with `@RegisterAiService`

## Development Patterns

### Adding New AI Features

1. Create an interface annotated with `@RegisterAiService`
2. Define methods with `@UserMessage` or `@SystemMessage` annotations
3. Inject the interface where needed - implementation is automatic

Example:

```java
@RegisterAiService
@ApplicationScoped
public interface MyAssistant {
    @UserMessage("Analyze this: {text}")
    String analyze(String text);
}
```

### Working with Embeddings

When storing documents:

- Always include metadata (sourceFile, filename, canonical flag)
- Use `canonical: true` for source material vs generated content
- Configure chunk size/overlap via `campaign.chunk.size` and `campaign.chunk.overlap`

### Configuration

Key settings in [application.properties](src/main/resources/application.properties):

- `quarkus.langchain4j.ollama.chat-model.model-name` - LLM for chat (default: mistral-nemo:12b)
- `quarkus.langchain4j.ollama.embedding-model.model-name` - Embedding model (default: nomic-embed-text)
- `campaign.chunk.size` / `campaign.chunk.overlap` - Document chunking parameters
- Neo4j connection and credentials

Quarkus configuration follows standard patterns:
- `application.properties` for default settings
- Environment variables or system properties for overrides
- `@ConfigProperty` for injection

## Project Structure

```
src/main/java/dev/ebullient/soloplay/
├── ai/                        # AI/LangChain4j components
│   ├── ChatAssistant.java     # AI chat interface (auto-implemented)
│   ├── SettingAssistant.java  # RAG query interface (auto-implemented)
│   └── StoryTools.java        # AI tools for story management
├── api/                       # REST API endpoints (Quarkus REST)
├── data/                      # Domain models (Character, Location, Event, etc.)
├── web/                       # Web UI controllers (Renarde MVC)
├── IngestService.java         # Document ingestion & embeddings
└── StoryRepository.java       # Story data access layer
src/main/resources/
├── META-INF/resources         # static resources (Renarde front-end)
└── templates                  # Qute templates for Renarde views and tool responses
```

**Key Directories:**
- `ai/` - LangChain4j AI services and tools (assistants, tools, retrievers)
- `api/` - REST API endpoints using Quarkus REST
- `data/` - Domain models and Neo4j OGM entities
- `web/` - Web UI controllers using Renarde MVC

## API Reference

The application provides both REST APIs and web UI (Renarde MVC) for interacting with features.

### Chat

**Purpose:** Generic LLM chat without RAG or story context

- **REST API:** `GET/POST /api/chat`
  - Query param or body: `question` (string)
  - Returns: HTML (markdown converted)
- **Web UI:** `/chat`
  - Interactive chat page using REST API

### Lore (RAG Queries)

**Purpose:** Query campaign documents with semantic search and LLM augmentation

- **REST API:**
  - `GET/POST /api/lore` - Query lore with question
  - `POST /api/lore/ingest` - Upload campaign documents (multipart form)
  - `DELETE /api/lore/all` - Delete all documents
  - `DELETE /api/lore/files?sourceFile=...` - Delete specific file
- **Web UI:**
  - `/lore` - Lore query interface
  - `/ingest` - Document upload and management
- **Implementation:** Uses LoreAssistant (RAG), retrieves from Neo4j embeddings

### Story / Solo Play

**Purpose:** Story-aware gameplay with character/location/event management

- **REST API:**
  - `GET /api/story/list` - List all story thread IDs
  - `POST /api/story/{threadId}/chat` - Story-aware chat with tools
  - Story data CRUD operations available
- **Web UI:**
  - `/story` - Story thread selection and creation
  - `/story/{threadId}/play` - Main gameplay interface
  - `/inspect` - View/manage story data (characters, locations, events)
    - "Inspect" provides CRUD views over campaign data - SPOILERS!
- **Implementation:**
  - Uses PlayAssistant (RAG + tools + thread context)
  - AI can invoke StoryTools to manage characters, locations, events
  - StoryRepository provides data access layer
  - Web controllers can call StoryTools directly or via REST

### Architecture Notes

**Web UI → Data Access Patterns:**
- **Chat/Lore web pages** → Call REST APIs via JavaScript
- **Story web pages** → Mix of direct StoryRepository/StoryTools calls and REST API
- **Inspect pages** → Direct StoryRepository/StoryTools access

**AI Tool Access:**
- LLM can autonomously invoke StoryTools methods as tools
- Tools are available to PlayAssistant (story-aware chat)
- Web UI can also call the same tool methods directly

## Data Flows

**Document Flow:**
```
Upload → Parse (YAML + chunks) → Embed (nomic-embed-text) → Store (Neo4j)
```

**Query Flow:**
```
User Question → Retrieve (Neo4j vector search) → Augment → LLM → Response
```

**Tool Flow:**
```
AI Response → Tool Call → Java Method → Neo4j → Result → Continue Generation
```

## AI-Assisted Contributions

We welcome contributions, including those created with AI assistance. Please ensure:

- **You understand the changes**: Be able to explain the rationale for your decisions
- **You've tested locally**: Run tests and verify functionality with local Ollama/Neo4j
- **Changes fit existing patterns**: Follow the architectural patterns described above
- **You've read the AI guidelines**: See [CLAUDE.md](CLAUDE.md) for AI assistant working guidelines

Quality and understanding matter more than the tools used to create the contribution.
