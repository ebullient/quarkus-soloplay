# Neo4j Data Migrations

This document contains Cypher scripts for migrating existing data when the schema changes.

## Common Warnings (Harmless)

### "Property name not available in database" Warning on Fresh Database

**Symptom**: When starting a new story thread (with no characters created yet), you may see:
```
WARN [org.neo4j.ogm.drivers.bolt.response.BoltResponse.unrecognized]
One of the property names in your query is not available in the database
(the missing property name is: storyThreadId)
```

**Cause**: The GM system prompt instructs the AI to query `getPartyMembers()` before responding. When no Character nodes exist in the database yet, Neo4j doesn't recognize the `storyThreadId` property in the query schema.

**Resolution**: **This warning is completely harmless** and will disappear once the first Character is created. Neo4j is just informing that the property doesn't exist in the schema yet because no nodes of that type have been persisted. The query still works correctly (returns empty results).

**To suppress the warning** (optional), add this to `application.properties`:
```properties
# Suppress Neo4j OGM unrecognized property warnings
quarkus.log.category."org.neo4j.ogm.drivers.bolt.response.BoltResponse".level=ERROR
```

---

## Migration 1: Add storyThreadId to Character Nodes

**Issue**: Character nodes created before the `storyThreadId` property was added to the entity don't have this field populated, causing the same warning on databases with existing data.

**Symptom**: Same warning as above, but persists even after creating characters.

**Diagnosis**: Check if existing characters are missing the property:
```cypher
MATCH (c:Character)
WHERE c.storyThreadId IS NULL AND c.id IS NOT NULL
RETURN c.id, c.name, c.storyThreadId
LIMIT 10
```

**Fix**: Extract `storyThreadId` from the composite `id` field (format: `{storyThreadId}:{characterSlug}`):

```cypher
// Check characters missing storyThreadId
MATCH (c:Character)
WHERE c.storyThreadId IS NULL AND c.id IS NOT NULL
RETURN c.id, c.name, c.storyThreadId
LIMIT 10
```

```cypher
// Populate storyThreadId from the composite id
MATCH (c:Character)
WHERE c.storyThreadId IS NULL AND c.id CONTAINS ":"
SET c.storyThreadId = split(c.id, ":")[0]
RETURN COUNT(c) as updated
```

**Verify**:
```cypher
MATCH (c:Character)
WHERE c.storyThreadId IS NULL
RETURN COUNT(c) as missing_story_thread_id
// Should return 0
```

## Migration 2: Add storyThreadId to Location Nodes

**Same issue may apply to Locations**:

```cypher
// Check locations missing storyThreadId
MATCH (l:Location)
WHERE l.storyThreadId IS NULL AND l.id IS NOT NULL
RETURN l.id, l.name, l.storyThreadId
LIMIT 10
```

```cypher
// Populate storyThreadId from the composite id
MATCH (l:Location)
WHERE l.storyThreadId IS NULL AND l.id CONTAINS ":"
SET l.storyThreadId = split(l.id, ":")[0]
RETURN COUNT(l) as updated
```

## Migration 3: Add storyThreadId to StoryEvent Nodes

**Same pattern for events**:

```cypher
// Populate storyThreadId from the composite id
MATCH (e:StoryEvent)
WHERE e.storyThreadId IS NULL AND e.id CONTAINS ":"
SET e.storyThreadId = split(e.id, ":")[0]
RETURN COUNT(e) as updated
```

## Running Migrations

### Option 1: Neo4j Browser
1. Open Neo4j Browser at http://localhost:7474
2. Paste and run each Cypher query
3. Verify the results

### Option 2: cypher-shell (if available)
```bash
cypher-shell -u neo4j -p password < migration.cypher
```

### Option 3: Via Application Endpoint (future)
Create a `/admin/migrate` endpoint that runs these scripts on startup or on-demand.

## Prevention

**Going forward**, ensure all entity creation paths set `storyThreadId` explicitly:

```java
// Character creation
Character character = new Character(storyThreadId, slug, name);
// The constructor sets both id and storyThreadId

// Location creation
Location location = new Location(storyThreadId, slug, name);
// Same pattern

// Event creation
StoryEvent event = new StoryEvent(storyThreadId, summary);
// Same pattern
```

All entities should initialize `storyThreadId` in their constructors to prevent this issue.
