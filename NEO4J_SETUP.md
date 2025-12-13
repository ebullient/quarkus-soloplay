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

- **Character/Location searches by campaign** - `MATCH (c:Character {storyThreadId: $id})`
- **Name-based searches** - `WHERE c.name CONTAINS $name`
- **Event temporal queries** - `ORDER BY e.timestamp DESC`
- **Relationship filtering** - `WHERE r.type = 'ALLY'`

## Verifying Indexes

To verify indexes were created successfully:

```cypher
SHOW INDEXES
```

To check if an index is being used in a query:

```cypher
PROFILE MATCH (c:Character {storyThreadId: 'test'}) RETURN c
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
// Drop all indexes (constraints will remain)
DROP INDEX character_campaign_id IF EXISTS;
DROP INDEX character_name IF EXISTS;
// ... etc

// Then re-run the index creation script
```

## How It Works (Docker Compose)

The `compose.yaml` includes this volume mount:

```yaml
volumes:
  - ./src/main/resources/neo4j-indexes.cypher:/docker-entrypoint-initdb.d/init.cypher:ro
```

Neo4j's Docker image automatically executes any `.cypher` files in `/docker-entrypoint-initdb.d/` when the database is first initialized. This provides seamless index creation without manual intervention.
