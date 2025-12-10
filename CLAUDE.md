# AI Assistant Working Guidelines

**For complete build commands, architecture overview, and development setup, see [CONTRIBUTING.md](CONTRIBUTING.md).**

## Your Role

Act as a pair programming partner with these responsibilities:

- **REVIEW THOROUGHLY**: Read existing code before making changes
    - Understand the AI Services pattern (auto-implemented interfaces)
    - Check how existing features use LangChain4j annotations
    - ASK FOR CLARIFICATION if implementation choices are unclear
- **BE EFFICIENT**: Be succinct and concise
- **RESPECT PRIVACY**: Do not read .env* files unless instructed
- **NO SPECULATION**: Never make up code or guess at API behavior

## Quick Reference

**Commands:**

```bash
./mvnw quarkus:dev              # Dev mode with live reload
./mvnw test                     # Run tests
./mvnw test -Dtest=ClassName    # Single test
```

**Key Files:**

- [CampaignResource.java](src/main/java/dev/ebullient/soloplay/CampaignResource.java) - REST endpoints
- [CampaignService.java](src/main/java/dev/ebullient/soloplay/CampaignService.java) - Document processing & embeddings
- [ChatAssistant.java](src/main/java/dev/ebullient/soloplay/ChatAssistant.java) - AI chat interface
- [SettingAssistant.java](src/main/java/dev/ebullient/soloplay/SettingAssistant.java) - RAG lore queries
- [application.properties](src/main/resources/application.properties) - Configuration

## Key Development Principles

**Follow the AI Services Pattern:**

- AI interfaces are annotated with `@RegisterAiService` - they are auto-implemented by LangChain4j
- Methods use `@UserMessage` or `@SystemMessage` annotations
- Don't try to implement these interfaces manually - Quarkus does it at build time

**Understand the Document Processing Pipeline:**

1. Files uploaded via multipart form → CampaignService
2. Content parsed (YAML frontmatter extracted, text chunked)
3. Embeddings generated via Ollama's nomic-embed-text
4. Stored in Neo4j with metadata

**Configuration-Driven:**

- Chunk size/overlap: `campaign.chunk.size` and `campaign.chunk.overlap`
- Ollama models: `quarkus.langchain4j.ollama.chat-model.model-name` and `embedding-model.model-name`
- Neo4j dimension must match embedding model (768 for nomic-embed-text)

## Important Patterns

**Quarkus REST (not RESTEasy):**

- Use `@RestQuery`, `@RestForm` from `org.jboss.resteasy.reactive`
- This project uses `quarkus-rest`, not the legacy `quarkus-resteasy`

**Metadata for Embeddings:**

- Always include: `settingName`, `sourceFile`, `canonical` flag
- Use `canonical: true` for source material vs generated content

**Response Augmentation:**

- All AI responses go through ResponseAugmenter
- Converts markdown → HTML for display

## Known Issues

- [CampaignResource.java:72](src/main/java/dev/ebullient/soloplay/CampaignResource.java:72) - Missing return statement in `loadSetting()` after the for loop

## When Making Changes

1. **Read similar code first**: Find existing patterns and follow them
2. **Understand the flow**: User request → REST → AI Service → LangChain4j → Ollama → Response
3. **Check configuration**: Many behaviors are configurable via application.properties
4. **Test with real services**: Requires Ollama and Neo4j running locally
