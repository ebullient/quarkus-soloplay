// Neo4j Index Creation Script
// Run these commands in Neo4j Browser or via cypher-shell to optimize query performance

// ===== Character Indexes =====

// Index on campaignId for listing characters by campaign
CREATE INDEX character_campaign_id IF NOT EXISTS
FOR (c:Character) ON (c.campaignId);

// Index on character name for search operations
CREATE INDEX character_name IF NOT EXISTS
FOR (c:Character) ON (c.name);

// Composite index for campaign + name searches
CREATE INDEX character_campaign_name IF NOT EXISTS
FOR (c:Character) ON (c.campaignId, c.name);

// Index on character ID (primary key - usually already indexed by OGM)
CREATE CONSTRAINT character_id_unique IF NOT EXISTS
FOR (c:Character) REQUIRE c.id IS UNIQUE;

// ===== Location Indexes =====

// Index on campaignId for listing locations by campaign
CREATE INDEX location_campaign_id IF NOT EXISTS
FOR (l:Location) ON (l.campaignId);

// Index on location name for search operations
CREATE INDEX location_name IF NOT EXISTS
FOR (l:Location) ON (l.name);

// Composite index for campaign + name searches
CREATE INDEX location_campaign_name IF NOT EXISTS
FOR (l:Location) ON (l.campaignId, l.name);

// Index on location ID (primary key)
CREATE CONSTRAINT location_id_unique IF NOT EXISTS
FOR (l:Location) REQUIRE l.id IS UNIQUE;

// ===== Event Indexes =====

// Index on campaignId for listing events by campaign
CREATE INDEX event_campaign_id IF NOT EXISTS
FOR (e:Event) ON (e.campaignId);

// Index on event timestamp for temporal queries
CREATE INDEX event_timestamp IF NOT EXISTS
FOR (e:Event) ON (e.timestamp);

// Index on conversationId for narrative thread tracking
CREATE INDEX event_conversation_id IF NOT EXISTS
FOR (e:Event) ON (e.conversationId);

// Composite index for campaign + timestamp (most common query)
CREATE INDEX event_campaign_timestamp IF NOT EXISTS
FOR (e:Event) ON (e.campaignId, e.timestamp);

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
// 1. Character/Location searches by campaign
// 2. Character/Location name searches (CONTAINS queries)
// 3. Event temporal queries
// 4. Relationship filtering by type/strength
// 5. Campaign-scoped queries (most common pattern)

// To verify indexes were created:
// SHOW INDEXES

// To check index usage in queries:
// PROFILE MATCH (c:Character {campaignId: 'test'}) RETURN c
