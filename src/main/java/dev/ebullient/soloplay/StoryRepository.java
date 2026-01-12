package dev.ebullient.soloplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.CharacterRelationship;
import dev.ebullient.soloplay.data.ConversationMessage;
import dev.ebullient.soloplay.data.Location;
import dev.ebullient.soloplay.data.StoryEvent;
import dev.ebullient.soloplay.data.StoryThread;
import io.quarkus.logging.Log;

/**
 * Repository providing data access operations for story data.
 * Used by both web controllers and AI tool services.
 */
@ApplicationScoped
public class StoryRepository {

    @Inject
    SessionFactory sessionFactory;

    // ===== HELPER METHODS =====

    /**
     * Convert an Iterable to a List.
     */
    private <T> List<T> toList(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isPresent(Integer value) {
        return value != null;
    }

    // ===== CHARACTER METHODS =====

    /**
     * Create a new character in the story thread with optional tags and aliases.
     * Id (slug-style) is auto-generated from the name.
     */
    public Character createCharacter(String storyThreadId, String name, String summary, String description,
            List<String> tags, List<String> aliases) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = new Character(storyThreadId, name);
            if (isPresent(summary)) {
                character.setSummary(summary);
            }
            if (isPresent(description)) {
                character.setDescription(description);
            }
            if (tags != null && !tags.isEmpty()) {
                // Clear default "npc" tag if specific tags are provided
                character.getTags().clear();
                tags.forEach(character::addTag);
            }
            if (aliases != null && !aliases.isEmpty()) {
                aliases.forEach(character::addAlias);
            }

            session.save(character);
            transaction.commit();
            return character;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Update a character's details.
     * Note: Alignment is now handled via tags (e.g., "alignment:lawful-good")
     */
    public Character updateCharacter(String characterId, String name, String summary, String description,
            String characterClass, Integer level) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = session.load(Character.class, characterId);
            if (character == null) {
                return null;
            }

            if (isPresent(name)) {
                character.setName(name);
            }
            if (isPresent(summary)) {
                character.setSummary(summary);
            }
            if (isPresent(description)) {
                character.setDescription(description);
            }
            if (isPresent(characterClass)) {
                character.setCharacterClass(characterClass);
            }
            if (isPresent(level)) {
                character.setLevel(level);
            }

            session.save(character);
            transaction.commit();
            return character;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Add tags to a character.
     */
    public Character addCharacterTags(String characterId, List<String> tags) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = session.load(Character.class, characterId);
            if (character == null) {
                return null;
            }

            tags.forEach(character::addTag);

            session.save(character);
            transaction.commit();
            return character;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Remove tags from a character.
     */
    public Character removeCharacterTags(String characterId, List<String> tags) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = session.load(Character.class, characterId);
            if (character == null) {
                return null;
            }

            tags.forEach(character::removeTag);

            session.save(character);
            transaction.commit();
            return character;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Replace all tags on a character with the given set.
     */
    public Character setCharacterTags(String characterId, Set<String> newTags) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = session.load(Character.class, characterId);
            if (character == null) {
                return null;
            }

            // Clear existing and set new tags
            character.getTags().clear();
            newTags.forEach(character::addTag);

            session.save(character);
            transaction.commit();
            return character;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Add aliases to a character.
     */
    public Character addCharacterAliases(String characterId, List<String> aliases) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = session.load(Character.class, characterId);
            if (character == null) {
                return null;
            }

            aliases.forEach(character::addAlias);

            session.save(character);
            transaction.commit();
            return character;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Remove aliases from a character.
     */
    public Character removeCharacterAliases(String characterId, List<String> aliases) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = session.load(Character.class, characterId);
            if (character == null) {
                return null;
            }

            aliases.forEach(character::removeAlias);

            session.save(character);
            transaction.commit();
            return character;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Find a character by name or alias (case-insensitive).
     */
    public Character findCharacterByNameOrAlias(String storyThreadId, String nameOrAlias) {
        String normalized = nameOrAlias.trim().toLowerCase();
        String cypher = """
                MATCH (c:Character)
                WHERE c.storyThreadId = $storyThreadId
                  AND (toLower(c.name) = $name OR $name IN c.aliases)
                RETURN c
                LIMIT 1
                """;
        var session = sessionFactory.openSession();
        var results = toList(session.query(Character.class, cypher,
                Map.of("storyThreadId", storyThreadId, "name", normalized)));
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a character by ID.
     */
    public Character findCharacterById(String characterId) {
        var session = sessionFactory.openSession();
        return session.load(Character.class, characterId);
    }

    /**
     * Find all characters in a story thread.
     */
    public List<Character> findAllCharacters(String storyThreadId) {
        String cypher = """
                MATCH (c:Character)
                WHERE c.storyThreadId = $storyThreadId
                RETURN c
                ORDER BY c.createdAt
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(Character.class, cypher, Map.of("storyThreadId", storyThreadId)));
    }

    /**
     * Find characters that have ANY of the specified tags.
     */
    public List<Character> findCharactersByAnyTag(String storyThreadId, List<String> tags) {
        // Normalize tags to lowercase for case-insensitive matching
        List<String> normalizedTags = tags.stream()
                .map(t -> t.trim().toLowerCase())
                .toList();

        String cypher = """
                MATCH (c:Character)
                WHERE c.storyThreadId = $storyThreadId
                  AND any(tag IN c.tags WHERE tag IN $tags)
                RETURN c
                ORDER BY c.createdAt
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(Character.class, cypher,
                Map.of("storyThreadId", storyThreadId, "tags", normalizedTags)));
    }

    /**
     * Find characters that have ALL of the specified tags.
     */
    public List<Character> findCharactersByAllTags(String storyThreadId, List<String> tags) {
        List<String> normalizedTags = tags.stream()
                .map(t -> t.trim().toLowerCase())
                .toList();

        String cypher = """
                MATCH (c:Character)
                WHERE c.storyThreadId = $storyThreadId
                  AND all(tag IN $tags WHERE tag IN c.tags)
                RETURN c
                ORDER BY c.createdAt
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(Character.class, cypher,
                Map.of("storyThreadId", storyThreadId, "tags", normalizedTags)));
    }

    /**
     * Transfer control of a character between player and GM.
     */
    public Character setCharacterControl(String characterId, String control) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = session.load(Character.class, characterId);
            if (character == null) {
                return null;
            }

            // Remove existing control tags
            character.removeTag("player-controlled");
            character.removeTag("npc");

            // Add appropriate control tag
            if ("player".equalsIgnoreCase(control)) {
                character.addTag("player-controlled");
            } else {
                character.addTag("npc");
            }

            session.save(character);
            transaction.commit();
            return character;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Get all player characters in a story thread.
     */
    public List<Character> getPlayerCharacters(String storyThreadId) {
        return findCharactersByAnyTag(storyThreadId, List.of("player-controlled"));
    }

    /**
     * Get all party members (PCs and companions) in a story thread.
     */
    public List<Character> getPartyMembers(String storyThreadId) {
        return findCharactersByAnyTag(storyThreadId, List.of("player-controlled", "companion"));
    }

    /**
     * Find characters associated with a location.
     */
    public List<Character> findCharactersByLocation(String locationId) {
        String cypher = """
                MATCH (c:Character)-[r:AT_LOCATION]->(l:Location)
                WHERE l.id = $locationId
                RETURN c
                ORDER BY c.createdAt
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(Character.class, cypher, Map.of("locationId", locationId)));
    }

    // ===== LOCATION METHODS =====

    /**
     * Create a new location in the story thread.
     */
    public Location createLocation(String storyThreadId, String name, String summary, String description,
            List<String> tags) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Location location = new Location(storyThreadId, name);
            if (isPresent(summary)) {
                location.setSummary(summary);
            }
            if (isPresent(description)) {
                location.setDescription(description);
            }
            if (tags != null && !tags.isEmpty()) {
                tags.forEach(location::addTag);
            }

            session.save(location);
            transaction.commit();
            return location;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Update a location's details.
     */
    public Location updateLocation(String locationId, String name, String summary, String description) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Location location = session.load(Location.class, locationId);
            if (location == null) {
                return null;
            }

            if (isPresent(name)) {
                location.setName(name);
            }
            if (isPresent(summary)) {
                location.setSummary(summary);
            }
            if (isPresent(description)) {
                location.setDescription(description);
            }

            session.save(location);
            transaction.commit();
            return location;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Add tags to a location.
     */
    public Location addLocationTags(String locationId, List<String> tags) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Location location = session.load(Location.class, locationId);
            if (location == null) {
                return null;
            }

            tags.forEach(location::addTag);

            session.save(location);
            transaction.commit();
            return location;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Remove tags from a location.
     */
    public Location removeLocationTags(String locationId, List<String> tags) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Location location = session.load(Location.class, locationId);
            if (location == null) {
                return null;
            }

            tags.forEach(location::removeTag);

            session.save(location);
            transaction.commit();
            return location;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Find a location by ID.
     */
    public Location findLocationById(String locationId) {
        var session = sessionFactory.openSession();
        return session.load(Location.class, locationId);
    }

    /**
     * Find all locations in a story thread.
     */
    public List<Location> findLocationsByStoryThreadId(String storyThreadId) {
        String cypher = """
                MATCH (l:Location)
                WHERE l.storyThreadId = $storyThreadId
                RETURN l
                ORDER BY l.createdAt
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(Location.class, cypher, Map.of("storyThreadId", storyThreadId)));
    }

    /**
     * Find locations that have ANY of the specified tags.
     */
    public List<Location> findLocationsByAnyTag(String storyThreadId, List<String> tags) {
        List<String> normalizedTags = tags.stream()
                .map(t -> t.trim().toLowerCase())
                .toList();

        String cypher = """
                MATCH (l:Location)
                WHERE l.storyThreadId = $storyThreadId
                  AND any(tag IN l.tags WHERE tag IN $tags)
                RETURN l
                ORDER BY l.createdAt
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(Location.class, cypher,
                Map.of("storyThreadId", storyThreadId, "tags", normalizedTags)));
    }

    /**
     * Connect two locations with a directional relationship.
     */
    public void connectLocations(String fromLocationId, String toLocationId, String direction, String description) {
        String cypher = """
                MATCH (from:Location {id: $fromId})
                MATCH (to:Location {id: $toId})
                CREATE (from)-[r:CONNECTS {direction: $direction, description: $description}]->(to)
                """;
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            session.query(cypher, Map.of(
                    "fromId", fromLocationId,
                    "toId", toLocationId,
                    "direction", direction,
                    "description", description));
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    // ===== STORY EVENT METHODS =====

    /**
     * Create a new story event.
     */
    public StoryEvent createEvent(String storyThreadId, String title, String description, Long day,
            List<String> participantIds, List<String> locationIds, List<String> tags) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            StoryEvent event = new StoryEvent(storyThreadId, title);
            if (isPresent(description)) {
                event.setDescription(description);
            }
            if (day != null) {
                event.setDay(day);
            }
            if (tags != null && !tags.isEmpty()) {
                tags.forEach(event::addTag);
            }

            session.save(event);

            // Create relationships to participants and locations
            if (participantIds != null && !participantIds.isEmpty()) {
                for (String participantId : participantIds) {
                    String cypher = """
                            MATCH (e:StoryEvent {id: $eventId})
                            MATCH (c:Character {id: $characterId})
                            CREATE (c)-[:PARTICIPATED_IN]->(e)
                            """;
                    session.query(cypher, Map.of("eventId", event.getId(), "characterId", participantId));
                }
            }

            if (locationIds != null && !locationIds.isEmpty()) {
                for (String locationId : locationIds) {
                    String cypher = """
                            MATCH (e:StoryEvent {id: $eventId})
                            MATCH (l:Location {id: $locationId})
                            CREATE (e)-[:OCCURRED_AT]->(l)
                            """;
                    session.query(cypher, Map.of("eventId", event.getId(), "locationId", locationId));
                }
            }

            transaction.commit();
            return event;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Find events by tags.
     */
    public List<StoryEvent> findEventsByAnyTag(String storyThreadId, List<String> tags) {
        List<String> normalizedTags = tags.stream()
                .map(t -> t.trim().toLowerCase())
                .toList();

        String cypher = """
                MATCH (e:StoryEvent)
                WHERE e.storyThreadId = $storyThreadId
                  AND any(tag IN e.tags WHERE tag IN $tags)
                RETURN e
                ORDER BY e.day DESC, e.createdAt DESC
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(StoryEvent.class, cypher,
                Map.of("storyThreadId", storyThreadId, "tags", normalizedTags)));
    }

    /**
     * Find recent events in a story thread.
     */
    public List<StoryEvent> findRecentEvents(String storyThreadId, int limit) {
        String cypher = """
                MATCH (e:StoryEvent)
                WHERE e.storyThreadId = $storyThreadId
                RETURN e
                ORDER BY e.day DESC, e.createdAt DESC
                LIMIT $limit
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(StoryEvent.class, cypher,
                Map.of("storyThreadId", storyThreadId, "limit", limit)));
    }

    /**
     * Find shared events between two characters.
     */
    public List<StoryEvent> findSharedEvents(String character1Id, String character2Id) {
        String cypher = """
                MATCH (c1:Character {id: $char1Id})-[:PARTICIPATED_IN]->(e:StoryEvent)<-[:PARTICIPATED_IN]-(c2:Character {id: $char2Id})
                RETURN e
                ORDER BY e.day DESC, e.createdAt DESC
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(StoryEvent.class, cypher,
                Map.of("char1Id", character1Id, "char2Id", character2Id)));
    }

    // ===== RELATIONSHIP METHODS =====

    /**
     * Create a relationship between two characters.
     */
    public CharacterRelationship createRelationship(String character1Id, String character2Id, List<String> tags) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            CharacterRelationship relationship = new CharacterRelationship();
            Character char1 = session.load(Character.class, character1Id);
            Character char2 = session.load(Character.class, character2Id);

            relationship.setCharacter1(char1);
            relationship.setCharacter2(char2);

            if (tags != null && !tags.isEmpty()) {
                tags.forEach(relationship::addTag);
            }

            session.save(relationship);
            transaction.commit();
            return relationship;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Find all relationships for a character.
     */
    public List<CharacterRelationship> findRelationshipsByCharacterId(String characterId) {
        String cypher = """
                MATCH (c:Character {id: $characterId})-[r:HAS_RELATIONSHIP]-(other:Character)
                RETURN r
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(CharacterRelationship.class, cypher, Map.of("characterId", characterId)));
    }

    /**
     * Find all relationships in a story thread.
     */
    public List<CharacterRelationship> findRelationshipsByStoryThreadId(String storyThreadId) {
        String cypher = """
                MATCH (c1:Character)-[r:HAS_RELATIONSHIP]-(c2:Character)
                WHERE c1.storyThreadId = $storyThreadId
                RETURN r
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(CharacterRelationship.class, cypher, Map.of("storyThreadId", storyThreadId)));
    }

    // ===== STORY THREAD METHODS =====

    /**
     * Save a story thread.
     */
    public void saveStoryThread(StoryThread thread) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            session.save(thread);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Create and save a new story thread with validation.
     * Validates id uniqueness and sets optional adventure/followingMode fields.
     *
     * @param name Story thread display name
     * @param adventureName Optional adventure name (can be null or blank)
     * @param followingMode Optional following mode (can be null or blank, defaults to LOOSE if adventure specified)
     * @return The created story thread
     * @throws IllegalArgumentException if id (slug) already exists or followingMode is invalid
     */
    public StoryThread createStoryThread(String name, String adventureName, String followingMode) {
        // Create new thread with name
        StoryThread thread = new StoryThread(name);

        // Check if slug already exists
        if (findStoryThreadById(thread.getId()) != null) {
            throw new IllegalArgumentException(
                    "A story with the name '" + name + "' already exists. Please choose a different name.");
        }

        // Set optional adventure
        if (adventureName != null && !adventureName.isBlank()) {
            thread.setAdventureName(adventureName);

            // Default followingMode to LOOSE if adventure specified but no mode provided
            if (followingMode == null || followingMode.isBlank()) {
                thread.setFollowingMode(StoryThread.FollowingMode.LOOSE);
            }
        }

        // Set followingMode with validation
        if (followingMode != null && !followingMode.isBlank()) {
            try {
                thread.setFollowingMode(StoryThread.FollowingMode.valueOf(followingMode));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid following mode: " + followingMode
                        + ". Valid values: LOOSE, STRICT, INSPIRATION");
            }
        }

        saveStoryThread(thread);
        return thread;
    }

    /**
     * Find a story thread by slug (primary ID).
     */
    public StoryThread findStoryThreadById(String slug) {
        var session = sessionFactory.openSession();
        return session.load(StoryThread.class, slug);
    }

    /**
     * Find all story threads.
     */
    public List<StoryThread> findAllStoryThreads() {
        String cypher = """
                OPTIONAL MATCH (st:StoryThread)
                WHERE st IS NOT NULL
                RETURN st
                ORDER BY COALESCE(st.lastPlayedAt, st.createdAt, datetime()) DESC
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(StoryThread.class, cypher, Map.of()));
    }

    /**
     * Find active story threads.
     */
    public List<StoryThread> findActiveStoryThreads() {
        String cypher = """
                OPTIONAL MATCH (st:StoryThread)
                WHERE st.status = 'ACTIVE'
                RETURN st
                ORDER BY COALESCE(st.lastPlayedAt, st.createdAt, datetime()) DESC
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(StoryThread.class, cypher, Map.of()));
    }

    /**
     * Get list of all story thread IDs that have data.
     * Returns empty list if no story threads exist.
     */
    public List<String> getStoryThreadIds() {
        var session = sessionFactory.openSession();
        List<String> storyThreadIds = new ArrayList<>();

        try {
            // Query for distinct story thread IDs from all data types
            Iterable<Map<String, Object>> results = session.query(
                    "MATCH (n) WHERE n.storyThreadId IS NOT NULL " +
                            "RETURN DISTINCT n.storyThreadId as storyThreadId ORDER BY n.storyThreadId",
                    Map.of());

            results.forEach(row -> {
                String storyThreadId = (String) row.get("storyThreadId");
                if (isPresent(storyThreadId)) {
                    storyThreadIds.add(storyThreadId);
                }
            });
        } catch (Exception e) {
            // Log the warning but don't fail - this happens when no data exists yet
            Log.debugf("No story thread data found yet: %s", e.getMessage());
        }

        return storyThreadIds;
    }

    /**
     * Delete a story thread and all associated data (characters, locations, events, relationships).
     */
    public void deleteStoryThread(String slug) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            // Delete all entities with storyThreadId matching the slug
            String cypher = """
                    MATCH (n)
                    WHERE n.storyThreadId = $slug
                    DETACH DELETE n
                    """;
            session.query(cypher, Map.of("slug", slug));

            // Delete the story thread itself
            StoryThread thread = session.load(StoryThread.class, slug);
            if (thread != null) {
                session.delete(thread);
            }

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Delete a character and all associated relationships.
     */
    public void deleteCharacter(String characterId) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = session.load(Character.class, characterId);
            if (character != null) {
                session.delete(character);
            }
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Delete a location and all associated relationships.
     */
    public void deleteLocation(String locationId) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Location location = session.load(Location.class, locationId);
            if (location != null) {
                session.delete(location);
            }
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    // ===== CONVERSATION TRANSCRIPT METHODS =====

    /**
     * Add a message to the conversation transcript.
     * Sequence number is calculated atomically within the transaction to prevent race conditions.
     */
    public ConversationMessage addConversationMessage(String storyThreadId, String role,
            String markdown, String html) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            // Get next seq inside transaction to prevent race conditions
            Long seq = 1L;
            Iterable<Map<String, Object>> results = session.query(
                    "MATCH (m:ConversationMessage {storyThreadId: $storyThreadId}) " +
                            "RETURN COALESCE(MAX(m.seq), 0) + 1 AS nextSeq",
                    Map.of("storyThreadId", storyThreadId));
            for (Map<String, Object> row : results) {
                seq = ((Number) row.get("nextSeq")).longValue();
            }

            ConversationMessage message = new ConversationMessage(storyThreadId, seq, role, markdown, html);
            session.save(message);
            transaction.commit();
            return message;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Get conversation history for a story thread.
     * Returns messages ordered oldest to newest, limited to the last N messages.
     */
    public List<ConversationMessage> getConversationHistory(String storyThreadId, int limit) {
        var session = sessionFactory.openSession();
        // Get last N messages ordered by seq ascending (oldest first)
        Iterable<ConversationMessage> results = session.query(
                ConversationMessage.class,
                "MATCH (m:ConversationMessage {storyThreadId: $storyThreadId}) " +
                        "RETURN m ORDER BY m.seq DESC LIMIT $limit",
                Map.of("storyThreadId", storyThreadId, "limit", limit));

        List<ConversationMessage> messages = toList(results);
        // Reverse to get oldest-first order
        java.util.Collections.reverse(messages);
        return messages;
    }

    /**
     * Delete all conversation messages for a story thread.
     */
    public void deleteConversationHistory(String storyThreadId) {
        var session = sessionFactory.openSession();
        session.query(
                "MATCH (m:ConversationMessage {storyThreadId: $storyThreadId}) DELETE m",
                Map.of("storyThreadId", storyThreadId));
    }
}
