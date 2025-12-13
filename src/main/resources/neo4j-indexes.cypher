// Neo4j Index Creation Script
// Run these commands in Neo4j Browser or via cypher-shell to optimize query performance

// ===== Character Indexes =====

// Index on storyThreadId for listing characters by story_thread
CREATE INDEX character_story_thread_id IF NOT EXISTS
FOR (c:Character) ON (c.storyThreadId);

// Index on character name for search operations
CREATE INDEX character_name IF NOT EXISTS
FOR (c:Character) ON (c.name);

// Composite index for story_thread + name searches
CREATE INDEX character_story_thread_name IF NOT EXISTS
FOR (c:Character) ON (c.storyThreadId, c.name);

// Index on character ID (primary key - usually already indexed by OGM)
CREATE CONSTRAINT character_id_unique IF NOT EXISTS
FOR (c:Character) REQUIRE c.id IS UNIQUE;

// ===== Location Indexes =====

// Index on storyThreadId for listing locations by story_thread
CREATE INDEX location_story_thread_id IF NOT EXISTS
FOR (l:Location) ON (l.storyThreadId);

// Index on location name for search operations
CREATE INDEX location_name IF NOT EXISTS
FOR (l:Location) ON (l.name);

// Composite index for story_thread + name searches
CREATE INDEX location_story_thread_name IF NOT EXISTS
FOR (l:Location) ON (l.storyThreadId, l.name);

// Index on location ID (primary key)
CREATE CONSTRAINT location_id_unique IF NOT EXISTS
FOR (l:Location) REQUIRE l.id IS UNIQUE;

// ===== Event Indexes =====

// Index on storyThreadId for listing events by story_thread
CREATE INDEX event_story_thread_id IF NOT EXISTS
FOR (e:Event) ON (e.storyThreadId);

// Index on event timestamp for temporal queries
CREATE INDEX event_timestamp IF NOT EXISTS
FOR (e:Event) ON (e.timestamp);

// Index on conversationId for narrative thread tracking
CREATE INDEX event_conversation_id IF NOT EXISTS
FOR (e:Event) ON (e.conversationId);

// Composite index for story_thread + timestamp (most common query)
CREATE INDEX event_story_thread_timestamp IF NOT EXISTS
FOR (e:Event) ON (e.storyThreadId, e.timestamp);

// Index on event ID (primary key)
CREATE CONSTRAINT event_id_unique IF NOT EXISTS
FOR (e:Event) REQUIRE e.id IS UNIQUE;

// ===== Relationship Indexes =====

// Index on relationship type for filtering
CREATE INDEX relationship_type IF NOT EXISTS
FOR ()-[r:RELATES_TO]-() ON (r.type);

// Index on relationship strength for filtering
CREATE INDEX relationship_strength IF NOT EXISTS
FOR ()-[r:RELATES_TO]-() ON (r.strength);

// ===== Performance Notes =====

// These indexes will significantly improve:
// 1. Character/Location searches by story_thread
// 2. Character/Location name searches (CONTAINS queries)
// 3. Event temporal queries
// 4. Relationship filtering by type/strength
// 5. Campaign-scoped queries (most common pattern)

// To verify indexes were created:
// SHOW INDEXES

// To check index usage in queries:
// PROFILE MATCH (c:Character {storyThreadId: 'test'}) RETURN c
