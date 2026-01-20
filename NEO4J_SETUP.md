# Neo4j Setup and Optimization

## Database Indexes

Indexes are automatically created when using Docker Compose for optimal query performance.

## Automatic Index Creation (Docker Compose)

When you start Neo4j using `docker compose up`, indexes are **automatically applied** on first startup:

```bash
docker compose up -d
```

The `compose.yaml` mounts `src/main/resources/neo4j-indexes.cypher` to `/docker-entrypoint-initdb.d/init.cypher`, which Neo4j executes during initialization.

**Important**: This only runs when creating a fresh database. If you already have data, see below.

## Apply Indexes to Existing Database

If you already have a running Neo4j database with data:

```bash
# Apply indexes using docker compose
cat src/main/resources/neo4j-indexes.cypher | \
  docker compose exec -T neo4j cypher-shell -u neo4j -p devpassword
```

### Alternative: Neo4j Browser

1. Open Neo4j Browser at `http://localhost:7474`
2. Connect using credentials (`neo4j`/`devpassword`)
3. Copy and paste the contents of `src/main/resources/neo4j-indexes.cypher`
4. Execute the script

## Reset Database (Fresh Start)

To start completely fresh and trigger automatic index creation:

```bash
docker compose down -v  # WARNING: Deletes all data!
docker compose up -d    # Recreates database with indexes auto-applied
```

## Index Benefits

The indexes optimize these common operations:

- **File listing** - `MATCH (d:Document) WHERE d.sourceFile IS NOT NULL RETURN d.sourceFile, count(*)`
- **Cross-reference lookups** - `MATCH (d:Document) WHERE d.filename = $filename RETURN d.text ORDER BY d.sectionIndex, d.chunkIndex`
- **Adventure lists** - `MATCH (d:Document) WHERE d.adventureName IS NOT NULL RETURN DISTINCT d.adventureName`

## Verifying Indexes

To verify indexes were created successfully:

```cypher
SHOW INDEXES
```

To check if an index is being used in a query:

```cypher
PROFILE MATCH (d:Document) WHERE d.filename = 'test.md' RETURN d.text ORDER BY d.sectionIndex, d.chunkIndex
```

Look for `NodeIndexSeek` in the query plan.

## Performance Monitoring

Monitor query performance with:

```cypher
// Show slow queries
CALL dbms.listQueries() YIELD query, elapsedTimeMillis
WHERE elapsedTimeMillis > 1000
RETURN query, elapsedTimeMillis
ORDER BY elapsedTimeMillis DESC
```

## Constraints vs Indexes

- **Constraints** ensure data integrity (uniqueness) AND create indexes
- **Indexes** only improve query performance
- This script uses both appropriately:
    - `CONSTRAINT` for ID fields (ensures uniqueness)
    - `INDEX` for search and filter fields

## Re-indexing

If you need to rebuild indexes:

```cypher
// Drop indexes (constraints will remain)
DROP INDEX document_source_file IF EXISTS;
DROP INDEX document_filename IF EXISTS;
DROP INDEX document_adventure_name IF EXISTS;
DROP INDEX document_filename_section_chunk IF EXISTS;

// Then re-run the index creation script
```
