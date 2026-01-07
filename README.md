# Soloplay

An AI-powered assistant for solo tabletop RPG gameplay, built with Quarkus and LangChain4j.

## What is Soloplay?

Soloplay helps you play tabletop RPGs solo by providing an AI game master assistant that:

- **Manages your campaign story** - Track characters, locations, events, and relationships in a graph database
- **Answers lore questions** - RAG (Retrieval Augmented Generation) queries against your uploaded campaign documents
- **Provides AI chat** - Interactive conversations with an AI assistant that has access to your story data
- **Uses AI tools** - The AI can create/update characters, locations, and events as the story unfolds

### Key Features

**Story Management:**
- Create and track characters with flexible tag-based classification (NPCs, companions, quest-givers, etc.)
- Manage locations with relationships to characters and events
- Record story events and their connections to the narrative
- Model character relationships with multi-dimensional tags (ally, friend, trusts, etc.)

**Document Ingestion:**
- Upload campaign setting documents (markdown with YAML frontmatter)
- Automatic text chunking and embedding generation
- Vector storage in Neo4j for semantic search

**RAG-Powered Queries:**
- Ask questions about your campaign lore
- AI retrieves relevant passages from your documents
- Answers grounded in your source material

**AI Tools Integration:**
- AI can autonomously create/update story elements
- Tools for character management, location tracking, and event recording
- Searchable story graph with tag-based queries

## Quick Start

### Prerequisites

1. **Java 21** or later
2. **Ollama** with required models:
   ```bash
   ollama pull mistral-nemo:12b
   ollama pull nomic-embed-text
   ```
3. **Neo4j** database (Docker recommended):
   ```bash
   docker run -d \
     --name neo4j \
     -p 7474:7474 -p 7687:7687 \
     -e NEO4J_AUTH=neo4j/password \
     neo4j:latest
   ```

### Running

```bash
# Start in dev mode with live reload
./mvnw quarkus:dev
```

The application will be available at:
- Main UI: <http://localhost:8080>
- Dev UI: <http://localhost:8080/q/dev/>

### First Steps

1. Navigate to the ingestion page to upload campaign documents
2. Use the lore query interface to ask questions about your setting
3. Start a chat session and let the AI create characters and locations as you play

## Use Cases

**Solo RPG Play:**
- Use as a GM assistant for solo campaigns
- Let the AI track NPCs, locations, and plot threads
- Query your campaign lore during play
- Generate responses to player actions

**Campaign Preparation:**
- Upload setting documents and let the AI answer questions
- Build a graph of campaign elements before play
- Explore relationships between characters and locations

**Worldbuilding:**
- Maintain consistency across large campaign settings
- Track evolving character relationships
- Record events and their narrative impact

## Technology Stack

Built with Quarkus, LangChain4j, Ollama (local LLM), and Neo4j (graph database + vector storage).

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed architecture, API documentation, and development setup.

## Contributing

Contributions welcome! This project supports AI-assisted development.

- Read [CONTRIBUTING.md](CONTRIBUTING.md) for architecture and patterns
- Read [CLAUDE.md](CLAUDE.md) for AI assistant working guidelines
- Requires Ollama and Neo4j running locally for testing
- Quality and understanding matter more than the tools used

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Learn More

- [Quarkus](https://quarkus.io/)
- [LangChain4j](https://docs.langchain4j.dev/)
- [Ollama](https://ollama.ai/)
- [Neo4j](https://neo4j.com/)
