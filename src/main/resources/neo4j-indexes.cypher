// Neo4j schema setup (indexes/constraints)
// Run these commands in Neo4j Browser or via cypher-shell.

// ===== RAG Document Indexes =====

// Used by LoreRepository + IngestService (file listing, cross-reference lookup, and ordering).
CREATE INDEX document_source_file IF NOT EXISTS
FOR (d:Document) ON (d.sourceFile);

CREATE INDEX document_filename IF NOT EXISTS
FOR (d:Document) ON (d.filename);

CREATE INDEX document_adventure_name IF NOT EXISTS
FOR (d:Document) ON (d.adventureName);

// Speeds up document reconstruction by filename in section/chunk order.
CREATE INDEX document_filename_section_chunk IF NOT EXISTS
FOR (d:Document) ON (d.filename, d.sectionIndex, d.chunkIndex);

// ===== ChatMemory Indexes =====

// Unique constraint on chat memory ID
CREATE CONSTRAINT chat_memory_id_unique IF NOT EXISTS
FOR (m:ChatMemory) REQUIRE m.id IS UNIQUE;

// To verify indexes were created
SHOW INDEXES

// To check index usage in queries
// PROFILE MATCH (d:Document) WHERE d.filename = 'test.md' RETURN d.text ORDER BY d.sectionIndex, d.chunkIndex
