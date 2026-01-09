# AI Assistant Guidelines

**For architecture, build commands, and API reference, see [CONTRIBUTING.md](CONTRIBUTING.md).**

## Your Role

Act as a pair programming partner:

- **REVIEW FIRST**: Read existing code before making changes. Understand existing patterns.
- **BE EFFICIENT**: Be succinct and concise
- **RESPECT PRIVACY**: Do not read .env* files unless instructed
- **NO SPECULATION**: Never make up code or guess at API behavior
- **ASK**: If implementation choices are unclear, ask for clarification

## Quick Commands

```bash
./mvnw quarkus:dev              # Dev mode with live reload (ask first)
./mvnw test                     # Run tests
./mvnw test -Dtest=ClassName    # Single test
```

## Key Patterns

### Neo4j OGM Sessions

Sessions are opened via `sessionFactory.openSession()` WITHOUT explicit closing. Connection pooling is managed by the extension.

```java
public List<Movie> findAll() {
    Session session = sessionFactory.openSession();
    return session.loadAll(Movie.class);
    // No session.close() needed
}
```

- Read-only: Open session, execute, return (no transaction)
- Writes: Use `session.beginTransaction()`, commit/rollback, close transaction

**Common Warning (Harmless):** When starting a new story thread with no characters, you'll see a Neo4j OGM warning about `storyThreadId` property not found. This is expected and disappears after the first Character is created. See [docs/neo4j-migrations.md](docs/neo4j-migrations.md) for details.

### Renarde MVC

- [Controllers](https://docs.quarkiverse.io/quarkus-renarde/dev/concepts.html#controllers)
- [Flash scope](https://docs.quarkiverse.io/quarkus-renarde/dev/advanced.html#flash_scope)
- [Redirects/Routing](https://docs.quarkiverse.io/quarkus-renarde/dev/advanced.html#routing)

### Tag-Based Entity System

All entities (Characters, Locations, Events, Relationships) use flexible tags instead of rigid enums. Tags are case-insensitive, normalized to lowercase.

**Character Tags:**
- Control: `"player-controlled"`, `"npc"`
- Party: `"companion"`, `"temporary"`, `"protagonist"`
- Status: `"dead"`, `"missing"`, `"imprisoned"`, `"retired"`
- Role: `"quest-giver"`, `"merchant"`, `"villain"`, `"mentor"`
- Prefixed: `"faction:harpers"`, `"profession:blacksmith"`

**Location Tags:**
- Type: `"city"`, `"dungeon"`, `"wilderness"`, `"building"`
- Status: `"destroyed"`, `"abandoned"`, `"hidden"`
- Feature: `"fortified"`, `"magical"`, `"haunted"`
- Prefixed: `"climate:tropical"`, `"terrain:mountainous"`

**Event Tags:**
- Activity: `"combat"`, `"social"`, `"exploration"`, `"travel"`
- Quest: `"quest-start"`, `"quest-complete"`, `"clue-discovered"`
- Narrative: `"plot-twist"`, `"revelation"`, `"decision-made"`
- Prefixed: `"tone:dramatic"`, `"importance:critical"`

**Relationship Tags:**
- Social: `"ally"`, `"enemy"`, `"friend"`, `"rival"`
- Family: `"parent"`, `"child"`, `"sibling"`, `"spouse"`
- Professional: `"mentor"`, `"employer"`, `"colleague"`
- Emotional: `"trusts"`, `"fears"`, `"respects"`, `"despises"`

Tag operations follow consistent patterns: `create*()` accepts tags, `add*Tags()`/`remove*Tags()` manage them, `find*ByTags()` queries by ANY tag (OR).

## When Making Changes

1. **Read similar code first** - Find existing patterns and follow them
2. **Understand the flow** - User request → REST → AI Service → LangChain4j → Ollama → Response
3. **Check configuration** - Many behaviors are configurable via application.properties
4. **Test with real services** - Requires Ollama and Neo4j running locally
