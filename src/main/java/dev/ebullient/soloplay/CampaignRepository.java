package dev.ebullient.soloplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.neo4j.ogm.session.SessionFactory;

import dev.ebullient.soloplay.data.CampaignEvent;
import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.Character.CharacterType;
import dev.ebullient.soloplay.data.CharacterRelationship;
import dev.ebullient.soloplay.data.Location;

/**
 * Repository providing data access operations for campaign data.
 * Used by both web controllers and AI tool services.
 */
@ApplicationScoped
public class CampaignRepository {

    @Inject
    SessionFactory sessionFactory;

    // ===== CHARACTER METHODS =====

    /**
     * Create a new character in the campaign.
     */
    public Character createCharacter(String campaignId, String type, String name, String summary, String description) {
        var session = sessionFactory.openSession();

        Character character = new Character(campaignId, CharacterType.valueOf(type), name);
        if (summary != null && !summary.isBlank()) {
            character.setSummary(summary);
        }
        if (description != null && !description.isBlank()) {
            character.setDescription(description);
        }

        session.save(character);
        return character;
    }

    /**
     * Update a character's details.
     */
    public Character updateCharacter(String characterId, String name, String summary, String description,
            String characterClass, Integer level, String alignment, String status) {
        var session = sessionFactory.openSession();

        Character character = session.load(Character.class, characterId);
        if (character == null) {
            return null;
        }

        if (name != null && !name.isBlank())
            character.setName(name);
        if (summary != null)
            character.setSummary(summary);
        if (description != null)
            character.setDescription(description);
        if (characterClass != null)
            character.setCharacterClass(characterClass);
        if (level != null)
            character.setLevel(level);
        if (alignment != null)
            character.setAlignment(alignment);
        if (status != null)
            character.setStatus(Character.CharacterStatus.valueOf(status));

        session.save(character);
        return character;
    }

    /**
     * Find all characters for a campaign.
     */
    public List<Character> findCharactersByCampaignId(String campaignId) {
        var session = sessionFactory.openSession();
        Iterable<Character> result = session.query(Character.class,
                "MATCH (c:Character) WHERE c.campaignId = $campaignId RETURN c ORDER BY c.name",
                Map.of("campaignId", campaignId));

        List<Character> characters = new ArrayList<>();
        result.forEach(characters::add);
        return characters;
    }

    /**
     * Find characters by name (partial match).
     */
    public List<Character> findCharactersByNameContaining(String campaignId, String name) {
        var session = sessionFactory.openSession();
        Iterable<Character> result = session.query(Character.class,
                "MATCH (c:Character) WHERE c.campaignId = $campaignId AND c.name CONTAINS $name RETURN c",
                Map.of("campaignId", campaignId, "name", name));

        List<Character> characters = new ArrayList<>();
        result.forEach(characters::add);
        return characters;
    }

    /**
     * Find a character by ID.
     */
    public Character findCharacterById(String characterId) {
        var session = sessionFactory.openSession();
        return session.load(Character.class, characterId);
    }

    // ===== LOCATION METHODS =====

    /**
     * Create a new location in the campaign.
     */
    public Location createLocation(String campaignId, String type, String name, String summary, String description) {
        var session = sessionFactory.openSession();

        Location location = new Location(campaignId, Location.LocationType.valueOf(type), name);
        if (summary != null && !summary.isBlank()) {
            location.setSummary(summary);
        }
        if (description != null && !description.isBlank()) {
            location.setDescription(description);
        }

        session.save(location);
        return location;
    }

    /**
     * Update a location's details.
     */
    public Location updateLocation(String locationId, String name, String summary, String description) {
        var session = sessionFactory.openSession();

        Location location = session.load(Location.class, locationId);
        if (location == null) {
            return null;
        }

        if (name != null && !name.isBlank())
            location.setName(name);
        if (summary != null)
            location.setSummary(summary);
        if (description != null)
            location.setDescription(description);

        session.save(location);
        return location;
    }

    /**
     * Find all locations for a campaign.
     */
    public List<Location> findLocationsByCampaignId(String campaignId) {
        var session = sessionFactory.openSession();
        Iterable<Location> result = session.query(Location.class,
                "MATCH (l:Location) WHERE l.campaignId = $campaignId RETURN l ORDER BY l.name",
                Map.of("campaignId", campaignId));

        List<Location> locations = new ArrayList<>();
        result.forEach(locations::add);
        return locations;
    }

    /**
     * Find locations by name (partial match).
     */
    public List<Location> findLocationsByNameContaining(String campaignId, String name) {
        var session = sessionFactory.openSession();
        Iterable<Location> result = session.query(Location.class,
                "MATCH (l:Location) WHERE l.campaignId = $campaignId AND l.name CONTAINS $name RETURN l",
                Map.of("campaignId", campaignId, "name", name));

        List<Location> locations = new ArrayList<>();
        result.forEach(locations::add);
        return locations;
    }

    /**
     * Find a location by ID.
     */
    public Location findLocationById(String locationId) {
        var session = sessionFactory.openSession();
        return session.load(Location.class, locationId);
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
        Map<String, Object> parameters = Map.of("characterId", characterId);
        var session = sessionFactory.openSession();
        return new ArrayList<>((java.util.Collection<? extends CharacterRelationship>) session
                .query(CharacterRelationship.class, cypher, parameters));
    }

    /**
     * Find relationships between characters in a campaign (for network analysis).
     */
    public List<CharacterRelationship> findRelationshipsByCampaignId(String campaignId) {
        String cypher = """
                MATCH (from:Character {campaignId: $campaignId})-[r]-(to:Character {campaignId: $campaignId})
                RETURN r, startNode(r), endNode(r)
                ORDER BY r.since DESC
                """;
        Map<String, Object> parameters = Map.of("campaignId", campaignId);
        var session = sessionFactory.openSession();
        return new ArrayList<>((java.util.Collection<? extends CharacterRelationship>) session
                .query(CharacterRelationship.class, cypher, parameters));
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
        Map<String, Object> parameters = Map.of("locationId", locationId);
        var session = sessionFactory.openSession();
        return new ArrayList<>((java.util.Collection<? extends Character>) session.query(Character.class, cypher, parameters));
    }

    /**
     * Find events that multiple characters participated in together.
     */
    public List<CampaignEvent> findSharedEvents(String character1Id, String character2Id) {
        String cypher = """
                MATCH (c1:Character {id: $char1Id})-[:PARTICIPATED_IN]-(e:Event)-[:PARTICIPATED_IN]-(c2:Character {id: $char2Id})
                RETURN e
                ORDER BY e.timestamp DESC
                """;
        Map<String, Object> parameters = Map.of("char1Id", character1Id, "char2Id", character2Id);
        var session = sessionFactory.openSession();
        return new ArrayList<>(
                (java.util.Collection<? extends CampaignEvent>) session.query(CampaignEvent.class, cypher, parameters));
    }

    /**
     * Create a relationship between two characters.
     */
    public CharacterRelationship createRelationship(String fromCharacterId, String toCharacterId,
            String relationshipType, String description) {
        var session = sessionFactory.openSession();

        Character fromCharacter = session.load(Character.class, fromCharacterId);
        Character toCharacter = session.load(Character.class, toCharacterId);

        if (fromCharacter == null || toCharacter == null) {
            return null;
        }

        CharacterRelationship relationship = new CharacterRelationship(fromCharacter, toCharacter,
                CharacterRelationship.RelationType.valueOf(relationshipType));
        if (description != null && !description.isBlank()) {
            relationship.setDescription(description);
        }

        session.save(relationship);
        return relationship;
    }

    // ===== EVENT METHODS =====

    /**
     * Create a campaign event.
     */
    public CampaignEvent createEvent(String campaignId, String eventType, String title, String description) {
        var session = sessionFactory.openSession();

        CampaignEvent event = new CampaignEvent(campaignId, title, description);
        event.setType(CampaignEvent.EventType.valueOf(eventType));
        if (description != null && !description.isBlank()) {
            event.setDescription(description);
        }

        session.save(event);
        return event;
    }

    // ===== WEB CONTROLLER METHODS =====

    /**
     * Find characters for web display (ordered by creation date).
     */
    public List<Character> findCharactersByCampaignIdOrderByCreatedAt(String campaignId) {
        var session = sessionFactory.openSession();
        Iterable<Character> result = session.query(Character.class,
                "MATCH (c:Character) WHERE c.campaignId = $campaignId RETURN c ORDER BY c.createdAt DESC",
                Map.of("campaignId", campaignId));

        List<Character> characters = new ArrayList<>();
        result.forEach(characters::add);
        return characters;
    }

    /**
     * Find character by ID for web display.
     */
    public Character findCharacterByIdForWeb(String id) {
        var session = sessionFactory.openSession();
        return session.load(Character.class, id);
    }

    /**
     * Find locations for web display (ordered by creation date).
     */
    public List<Location> findLocationsByCampaignIdOrderByCreatedAt(String campaignId) {
        var session = sessionFactory.openSession();
        Iterable<Location> result = session.query(Location.class,
                "MATCH (l:Location) WHERE l.campaignId = $campaignId RETURN l ORDER BY l.createdAt DESC",
                Map.of("campaignId", campaignId));

        List<Location> locations = new ArrayList<>();
        result.forEach(locations::add);
        return locations;
    }

    /**
     * Find location by ID for web display.
     */
    public Location findLocationByIdForWeb(String id) {
        var session = sessionFactory.openSession();
        return session.load(Location.class, id);
    }

    /**
     * Get list of all campaign IDs that have data.
     */
    public List<String> getCampaignIds() {
        var session = sessionFactory.openSession();
        List<String> campaignIds = new ArrayList<>();

        try {
            // Query for distinct campaign IDs from all data types
            Iterable<Map<String, Object>> results = session.query(
                    "MATCH (n) WHERE n.campaignId IS NOT NULL " +
                            "RETURN DISTINCT n.campaignId as campaignId ORDER BY n.campaignId",
                    Map.of());

            results.forEach(row -> {
                String campaignId = (String) row.get("campaignId");
                if (campaignId != null && !campaignId.isBlank()) {
                    campaignIds.add(campaignId);
                }
            });
        } catch (Exception e) {
            // Log the warning but don't fail - this happens when no data exists yet
            System.out.println("No campaign data found yet: " + e.getMessage());
        }

        // Always ensure "default" is available as a starting point
        if (campaignIds.isEmpty() || !campaignIds.contains("default")) {
            campaignIds.add(0, "default");
        }

        return campaignIds;
    }
}