# Neo4j Constraints and Indexes

This document describes the database constraints that should be applied to ensure data integrity.

## Story Thread Slug Uniqueness

To enforce unique slugs for story threads, create a unique constraint in Neo4j:

### Using Cypher (Neo4j Browser or cypher-shell)

```cypher
CREATE CONSTRAINT story_thread_slug_unique IF NOT EXISTS
FOR (st:StoryThread) REQUIRE st.slug IS UNIQUE
```

### Verify the Constraint

```cypher
SHOW CONSTRAINTS
```

You should see output like:
```
name: "story_thread_slug_unique"
type: "UNIQUENESS"
entityType: "NODE"
labelsOrTypes: ["StoryThread"]
properties: ["slug"]
```

## Why This Matters

The application handles slug collisions in code by appending counters (e.g., "my-adventure-2"), but the database constraint provides an additional safety layer:

1. **Database-level enforcement** - Prevents race conditions if multiple threads try to create the same slug
2. **Index optimization** - Unique constraints automatically create an index, speeding up lookups
3. **Data integrity** - Ensures no duplicate slugs even if code has bugs

## When to Apply

Apply this constraint:
- **Before production use** - Ensures data integrity from the start
- **After development** - Can be added at any time (will fail if duplicates exist)

## Checking for Existing Duplicates

Before creating the constraint, check for existing duplicate slugs:

```cypher
MATCH (st:StoryThread)
WITH st.slug AS slug, COUNT(*) AS count
WHERE count > 1
RETURN slug, count
ORDER BY count DESC
```

If duplicates exist, you'll need to fix them first:

```cypher
// Example: Fix duplicate by appending counter
MATCH (st:StoryThread {slug: "my-adventure"})
WITH st, st.createdAt AS created
ORDER BY created
WITH COLLECT(st) AS threads
UNWIND RANGE(1, SIZE(threads)-1) AS idx
SET (threads[idx]).slug = threads[0].slug + "-" + (idx + 1)
```

## Additional Recommended Indexes

For optimal query performance, consider adding these indexes:

```cypher
// Index on status for finding active threads
CREATE INDEX story_thread_status IF NOT EXISTS
FOR (st:StoryThread) ON (st.status)

// Index on lastPlayedAt for sorting recent threads
CREATE INDEX story_thread_last_played IF NOT EXISTS
FOR (st:StoryThread) ON (st.lastPlayedAt)

// Index on settingName for filtering by setting
CREATE INDEX story_thread_setting IF NOT EXISTS
FOR (st:StoryThread) ON (st.settingName)
```

## Applying Constraints in Production

For production deployments, add these Cypher statements to your database initialization scripts or migration tools.
