package dev.ebullient.soloplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.CharacterRelationship;
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
     * Create a new character in the story thread with tags.
     */
    public Character createCharacter(String storyThreadId, String name, String summary, String description,
            List<String> tags) {
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
     */
    public Character updateCharacter(String characterId, String name, String summary, String description,
            String characterClass, Integer level, String alignment) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character character = session.load(Character.class, characterId);
            if (character == null) {
                transaction.rollback();
                return null;
            }

            if (isPresent(name))
                character.setName(name);
            if (isPresent(summary))
                character.setSummary(summary);
            if (isPresent(description))
                character.setDescription(description);
            if (isPresent(characterClass))
                character.setCharacterClass(characterClass);
            if (isPresent(level))
                character.setLevel(level);
            if (isPresent(alignment))
                character.setAlignment(alignment);

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
     * Find all characters for a story thread.
     */
    public List<Character> findCharactersByStoryThreadId(String storyThreadId) {
        var session = sessionFactory.openSession();
        return toList(session.query(Character.class,
                "MATCH (c:Character) WHERE c.storyThreadId = $storyThreadId RETURN c ORDER BY c.name",
                Map.of("storyThreadId", storyThreadId)));
    }

    /**
     * Find characters by name (partial match).
     */
    public List<Character> findCharactersByNameContaining(String storyThreadId, String name) {
        var session = sessionFactory.openSession();
        return toList(session.query(Character.class,
                "MATCH (c:Character) WHERE c.storyThreadId = $storyThreadId AND c.name CONTAINS $name RETURN c",
                Map.of("storyThreadId", storyThreadId, "name", name)));
    }

    /**
     * Find a character by ID.
     */
    public Character findCharacterById(String characterId) {
        var session = sessionFactory.openSession();
        return session.load(Character.class, characterId);
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
                transaction.rollback();
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
                transaction.rollback();
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
     * Find characters that have ANY of the specified tags (OR).
     */
    public List<Character> findCharactersByAnyTag(String storyThreadId, List<String> tags) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (c:Character)
                WHERE c.storyThreadId = $storyThreadId
                  AND any(tag IN $tags WHERE tag IN c.tags)
                RETURN c
                ORDER BY c.name
                """;
        return toList(session.query(Character.class, cypher,
                Map.of("storyThreadId", storyThreadId, "tags", tags)));
    }

    /**
     * Find characters that have ALL of the specified tags (AND).
     */
    public List<Character> findCharactersByAllTags(String storyThreadId, List<String> tags) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (c:Character)
                WHERE c.storyThreadId = $storyThreadId
                  AND all(tag IN $tags WHERE tag IN c.tags)
                RETURN c
                ORDER BY c.name
                """;
        return toList(session.query(Character.class, cypher,
                Map.of("storyThreadId", storyThreadId, "tags", tags)));
    }

    /**
     * Find characters that do NOT have any of the specified tags.
     */
    public List<Character> findCharactersExcludingTags(String storyThreadId, List<String> excludeTags) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (c:Character)
                WHERE c.storyThreadId = $storyThreadId
                  AND none(tag IN $excludeTags WHERE tag IN c.tags)
                RETURN c
                ORDER BY c.name
                """;
        return toList(session.query(Character.class, cypher,
                Map.of("storyThreadId", storyThreadId, "excludeTags", excludeTags)));
    }

    // ===== LOCATION METHODS =====

    /**
     * Create a new location in the story thread with tags.
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
                transaction.rollback();
                return null;
            }

            if (isPresent(name))
                location.setName(name);
            if (isPresent(summary))
                location.setSummary(summary);
            if (isPresent(description))
                location.setDescription(description);

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
     * Find all locations for a story thread.
     */
    public List<Location> findLocationsByStoryThreadId(String storyThreadId) {
        var session = sessionFactory.openSession();
        return toList(session.query(Location.class,
                "MATCH (l:Location) WHERE l.storyThreadId = $storyThreadId RETURN l ORDER BY l.name",
                Map.of("storyThreadId", storyThreadId)));
    }

    /**
     * Find locations by name (partial match).
     */
    public List<Location> findLocationsByNameContaining(String storyThreadId, String name) {
        var session = sessionFactory.openSession();
        return toList(session.query(Location.class,
                "MATCH (l:Location) WHERE l.storyThreadId = $storyThreadId AND l.name CONTAINS $name RETURN l",
                Map.of("storyThreadId", storyThreadId, "name", name)));
    }

    /**
     * Find a location by ID.
     */
    public Location findLocationById(String locationId) {
        var session = sessionFactory.openSession();
        return session.load(Location.class, locationId);
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
                transaction.rollback();
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
                transaction.rollback();
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
     * Find locations that have ANY of the specified tags (OR).
     */
    public List<Location> findLocationsByAnyTag(String storyThreadId, List<String> tags) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (l:Location)
                WHERE l.storyThreadId = $storyThreadId
                  AND any(tag IN $tags WHERE tag IN l.tags)
                RETURN l
                ORDER BY l.name
                """;
        return toList(session.query(Location.class, cypher,
                Map.of("storyThreadId", storyThreadId, "tags", tags)));
    }

    /**
     * Find locations that have ALL of the specified tags (AND).
     */
    public List<Location> findLocationsByAllTags(String storyThreadId, List<String> tags) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (l:Location)
                WHERE l.storyThreadId = $storyThreadId
                  AND all(tag IN $tags WHERE tag IN l.tags)
                RETURN l
                ORDER BY l.name
                """;
        return toList(session.query(Location.class, cypher,
                Map.of("storyThreadId", storyThreadId, "tags", tags)));
    }

    /**
     * Find locations that do NOT have any of the specified tags.
     */
    public List<Location> findLocationsExcludingTags(String storyThreadId, List<String> excludeTags) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (l:Location)
                WHERE l.storyThreadId = $storyThreadId
                  AND none(tag IN $excludeTags WHERE tag IN l.tags)
                RETURN l
                ORDER BY l.name
                """;
        return toList(session.query(Location.class, cypher,
                Map.of("storyThreadId", storyThreadId, "excludeTags", excludeTags)));
    }

    // ===== RELATIONSHIP METHODS =====

    /**
     * Find all relationships for a character (both incoming and outgoing).
     */
    public List<CharacterRelationship> findRelationshipsByCharacterId(String characterId) {
        String cypher = """
                MATCH (c:Character {id: $characterId})-[r]-(other:Character)
                RETURN r, startNode(r), endNode(r)
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(CharacterRelationship.class, cypher, Map.of("characterId", characterId)));
    }

    /**
     * Find relationships between characters in a story thread (for network analysis).
     */
    public List<CharacterRelationship> findRelationshipsByStoryThreadId(String storyThreadId) {
        String cypher = """
                MATCH (from:Character {storyThreadId: $storyThreadId})-[r]-(to:Character {storyThreadId: $storyThreadId})
                RETURN r, startNode(r), endNode(r)
                ORDER BY r.since DESC
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(CharacterRelationship.class, cypher, Map.of("storyThreadId", storyThreadId)));
    }

    /**
     * Find characters connected to a location (who have been there or live there).
     */
    public List<Character> findCharactersByLocation(String locationId) {
        String cypher = """
                MATCH (loc:Location {id: $locationId})-[:OCCURRED_AT]-(e:Event)-[:PARTICIPATED_IN]-(c:Character)
                RETURN DISTINCT c
                ORDER BY c.name
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(Character.class, cypher, Map.of("locationId", locationId)));
    }

    /**
     * Find events that multiple characters participated in together.
     */
    public List<StoryEvent> findSharedEvents(String character1Id, String character2Id) {
        String cypher = """
                MATCH (c1:Character {id: $char1Id})-[:PARTICIPATED_IN]-(e:Event)-[:PARTICIPATED_IN]-(c2:Character {id: $char2Id})
                RETURN e
                ORDER BY e.timestamp DESC
                """;
        var session = sessionFactory.openSession();
        return toList(session.query(StoryEvent.class, cypher,
                Map.of("char1Id", character1Id, "char2Id", character2Id)));
    }

    /**
     * Create a relationship between two characters.
     */
    public CharacterRelationship createRelationship(String fromCharacterId, String toCharacterId,
            String relationshipType, String description) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Character fromCharacter = session.load(Character.class, fromCharacterId);
            Character toCharacter = session.load(Character.class, toCharacterId);

            if (fromCharacter == null || toCharacter == null) {
                transaction.rollback();
                return null;
            }

            CharacterRelationship relationship = new CharacterRelationship(fromCharacter, toCharacter,
                    CharacterRelationship.RelationType.valueOf(relationshipType));
            if (isPresent(description)) {
                relationship.setDescription(description);
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

    // ===== EVENT METHODS =====

    /**
     * Create a story event.
     */
    public StoryEvent createEvent(String storyThreadId, String eventType, String title, String description) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            StoryEvent event = new StoryEvent(storyThreadId, title, description);
            event.setType(StoryEvent.EventType.valueOf(eventType));
            if (isPresent(description)) {
                event.setDescription(description);
            }

            session.save(event);
            transaction.commit();
            return event;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    // ===== STORY THREAD METHODS =====

    /**
     * Create a new story thread.
     */
    public StoryThread createStoryThread(String name, String settingName) {
        var session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            StoryThread storyThread = new StoryThread(name, settingName);
            session.save(storyThread);
            transaction.commit();
            return storyThread;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    /**
     * Find a story thread by ID.
     */
    public StoryThread findStoryThreadById(String id) {
        var session = sessionFactory.openSession();
        return session.load(StoryThread.class, id);
    }

    /**
     * Find all story threads.
     */
    public List<StoryThread> findAllStoryThreads() {
        var session = sessionFactory.openSession();
        return toList(session.loadAll(StoryThread.class));
    }

    /**
     * Find active story threads.
     */
    public List<StoryThread> findActiveStoryThreads() {
        String cypher = """
                MATCH (st:StoryThread)
                WHERE st.status = 'ACTIVE'
                RETURN st
                ORDER BY st.lastPlayedAt DESC
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
}
