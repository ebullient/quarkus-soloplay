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

- [ChatResource.java](src/main/java/dev/ebullient/soloplay/ChatResource.java) - Generic LLM chat API (/api/chat)
- [LoreResource.java](src/main/java/dev/ebullient/soloplay/LoreResource.java) - RAG lore query API (/api/lore)
- [StoryResource.java](src/main/java/dev/ebullient/soloplay/StoryResource.java) - Story-specific API (/api/story)
- [IngestService.java](src/main/java/dev/ebullient/soloplay/IngestService.java) - Document processing & embeddings
- [ChatAssistant.java](src/main/java/dev/ebullient/soloplay/ChatAssistant.java) - AI chat interface
- [SettingAssistant.java](src/main/java/dev/ebullient/soloplay/SettingAssistant.java) - RAG lore queries
- [StoryTools.java](src/main/java/dev/ebullient/soloplay/StoryTools.java) - AI tools for character/location/event management
- [StoryRepository.java](src/main/java/dev/ebullient/soloplay/StoryRepository.java) - Data access for story elements
- [application.properties](src/main/resources/application.properties) - Configuration

## Key Development Principles

**Follow the AI Services Pattern:**

- AI interfaces are annotated with `@RegisterAiService` - they are auto-implemented by LangChain4j
- Methods use `@UserMessage` or `@SystemMessage` annotations
- Don't try to implement these interfaces manually - Quarkus does it at build time

**Understand the Document Processing Pipeline:**

1. Files uploaded via multipart form → IngestService
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

**Tag-Based Entity System:**

All primary entities (Characters, Locations, Events, Relationships) use a flexible tag-based classification instead of rigid enums. Tags are case-insensitive and normalized to lowercase. Entities can have multiple tags.

**Character Tags:**

- **Control tags**: `"player-controlled"`, `"npc"` (default)
- **Party tags**: `"companion"`, `"temporary"`, `"protagonist"`
- **Status tags**: `"dead"`, `"missing"`, `"imprisoned"`, `"retired"`
- **Role tags**: `"quest-giver"`, `"merchant"`, `"informant"`, `"villain"`, `"mentor"`
- **Prefixed tags**: `"faction:thieves-guild"`, `"profession:blacksmith"`, `"location:tavern"`

Character tag operations:

- `createCharacter()` - accepts optional tags list
- `addCharacterTags()` / `removeCharacterTags()` - manage tags
- `findCharactersByTags()` - find by ANY tag (OR)
- `getPlayerCharacters()` - find player-controlled
- `getPartyMembers()` - find PCs + companions
- `setCharacterControl()` - transfer between player/GM control

**Location Tags:**

- **Type tags**: `"city"`, `"town"`, `"village"`, `"dungeon"`, `"wilderness"`, `"building"`, `"region"`, `"plane"`
- **Status tags**: `"destroyed"`, `"abandoned"`, `"hidden"`, `"active"`
- **Feature tags**: `"fortified"`, `"magical"`, `"haunted"`, `"sacred"`, `"cursed"`
- **Access tags**: `"public"`, `"restricted"`, `"secret"`, `"guarded"`
- **Prefixed tags**: `"faction:thieves-guild"`, `"climate:tropical"`, `"terrain:mountainous"`

Location tag operations:

- `createLocation()` - accepts optional tags list
- `addLocationTags()` / `removeLocationTags()` - manage tags
- `findLocationsByTags()` - find by ANY tag (OR)

**Event Tags:**

- **Activity tags**: `"combat"`, `"social"`, `"exploration"`, `"rest"`, `"travel"`
- **Quest tags**: `"quest-start"`, `"quest-complete"`, `"quest-failed"`, `"clue-discovered"`
- **Character tags**: `"character-death"`, `"level-up"`, `"character-introduced"`, `"character-departed"`
- **Location tags**: `"location-discovered"`, `"location-entered"`, `"location-left"`
- **Item tags**: `"item-acquired"`, `"item-lost"`, `"item-used"`, `"treasure-found"`
- **Narrative tags**: `"plot-twist"`, `"revelation"`, `"decision-made"`, `"consequence"`
- **Prefixed tags**: `"faction:thieves-guild"`, `"tone:dramatic"`, `"importance:critical"`

Event tag operations:

- `createEvent()` - accepts optional tags list
- `addEventTags()` / `removeEventTags()` - manage tags
- `findEventsByTags()` - find by ANY tag (OR)

**Relationship Tags:**

- **Social tags**: `"knows"`, `"ally"`, `"enemy"`, `"friend"`, `"rival"`, `"acquaintance"`
- **Family tags**: `"family"`, `"parent"`, `"child"`, `"sibling"`, `"spouse"`, `"ancestor"`
- **Professional tags**: `"mentor"`, `"student"`, `"employer"`, `"employee"`, `"colleague"`, `"business-partner"`
- **Romantic tags**: `"romantic"`, `"lover"`, `"ex-lover"`, `"courting"`, `"married"`
- **Organizational tags**: `"member-of"`, `"leader-of"`, `"subordinate"`, `"peer"`
- **Emotional tags**: `"trusts"`, `"distrusts"`, `"fears"`, `"respects"`, `"admires"`, `"despises"`
- **Prefixed tags**: `"faction:thieves-guild"`, `"status:secret"`, `"intensity:strong"`

Relationship tag operations:

- `createRelationship()` - accepts optional tags list
- `addRelationshipTags()` / `removeRelationshipTags()` - manage tags
- Relationships can have multiple dimensions (e.g., "ally" + "friend" + "trusts")

**Response Augmentation:**

- All AI responses go through ResponseAugmenter
- Converts markdown → HTML for display

**Neo4j OGM Session Management:**

- Neo4j OGM sessions are opened via `sessionFactory.openSession()` WITHOUT explicit closing
- Connection pooling and lifecycle are managed by the extension
- This is the documented pattern - see [movies-java-quarkus example](https://github.com/sdaschner/movies-java-quarkus)
- Example from official pattern:

  ```java
  @ApplicationScoped
  public class MovieRepository {
      @Inject SessionFactory sessionFactory;

      public List<Movie> findAll() {
          Session session = sessionFactory.openSession();
          return session.loadAll(Movie.class);
          // No session.close() needed
      }
  }
  ```

- Read-only queries: Open session, execute query, return results (no transaction needed)
- Write operations: Use `session.beginTransaction()`, commit/rollback as needed, close transaction
- The CampaignRepository follows this pattern correctly

**Renarde MVC**

- Controllers: https://docs.quarkiverse.io/quarkus-renarde/dev/concepts.html#controllers
- Flash scope: https://docs.quarkiverse.io/quarkus-renarde/dev/advanced.html#flash_scope
- Redirects: https://docs.quarkiverse.io/quarkus-renarde/dev/advanced.html#routing

## When Making Changes

1. **Read similar code first**: Find existing patterns and follow them
2. **Understand the flow**: User request → REST → AI Service → LangChain4j → Ollama → Response
3. **Check configuration**: Many behaviors are configurable via application.properties
4. **Test with real services**: Requires Ollama and Neo4j running locally
