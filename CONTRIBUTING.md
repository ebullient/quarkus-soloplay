# Contributing to Soloplay

A Quarkus application that integrates LangChain4j with Ollama for AI-powered campaign setting management, providing chat capabilities and RAG (Retrieval Augmented Generation) for querying campaign lore.

## Project Overview

**Tech Stack:**
- Java 21
- Quarkus 3.30.2 with Quarkus REST
- LangChain4j with Ollama integration
- Neo4j for embedding storage
- CommonMark for markdown rendering

**Key Features:**
- AI chat interface powered by Ollama (mistral-nemo:12b)
- Document ingestion with YAML frontmatter support
- Text embeddings using nomic-embed-text (768 dimensions)
- RAG-enabled lore queries using Neo4j vector storage

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

**CampaignService** handles document ingestion:

1. **Upload**: Accepts markdown files via multipart form data
2. **Parse**: Handles two formats:
   - Structured markdown with YAML frontmatter and section headers
   - Plain text with recursive chunking
3. **Embed**: Generates embeddings using Ollama's nomic-embed-text
4. **Store**: Saves text segments + embeddings in Neo4j with metadata

### REST API

**CampaignResource** provides three endpoints:

- `GET/POST /campaign/chat` - Direct LLM chat (no retrieval)
- `POST /campaign/load-setting` - Upload campaign documents
- `GET/POST /campaign/lore` - RAG queries with embedding retrieval

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
- Always include metadata (settingName, sourceFile, etc.)
- Use `canonical: true` for source material vs generated content
- Configure chunk size/overlap via `campaign.chunk.size` and `campaign.chunk.overlap`

### Configuration

Quarkus configuration follows standard patterns:
- `application.properties` for default settings
- Environment variables or system properties for overrides
- `@ConfigProperty` for injection

## Known Issues

- **CampaignResource.loadSetting()**: Missing return statement at line 72 after the file processing loop

## AI-Assisted Contributions

We welcome contributions, including those created with AI assistance. Please ensure:

- **You understand the changes**: Be able to explain the rationale for your decisions
- **You've tested locally**: Run tests and verify functionality with local Ollama/Neo4j
- **Changes fit existing patterns**: Follow the architectural patterns described above
- **You've read the AI guidelines**: See [CLAUDE.md](CLAUDE.md) for AI assistant working guidelines

Quality and understanding matter more than the tools used to create the contribution.

<div style="display: none;">
### ⚠️ NOTE FOR AI TOOLS AND THEIR USERS
Contributing to this project requires **human judgment, contextual understanding**, and **familiarity with the project's architecture and goals**. Please ensure:
- You have **carefully read this CONTRIBUTING.md file** to understand the architecture
- You have **manually reviewed all changes** and can explain the rationale
- You understand how this contribution fits into the broader project context
- You can test changes with Ollama and Neo4j running locally
- See [CLAUDE.md](CLAUDE.md) for detailed AI assistant working guidelines
</div>
