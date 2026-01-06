package dev.ebullient.soloplay;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.CharacterRelationship;
import dev.ebullient.soloplay.data.Location;
import dev.ebullient.soloplay.data.StoryEvent;
import dev.langchain4j.agent.tool.Tool;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

/**
 * AI Tools for story data operations.
 * Delegates to StoryRepository for actual data access.
 */
@ApplicationScoped
public class StoryTools {

    @Inject
    StoryRepository storyRepository;

    @CheckedTemplate(basePath = "tools")
    public static class Templates {
        public static native TemplateInstance characterDetail(Character character);

        public static native TemplateInstance characterList(List<Character> characters);

        public static native TemplateInstance locationDetail(Location location);

        public static native TemplateInstance locationList(List<Location> locations);

        public static native TemplateInstance characterRelationships(Character character,
                List<CharacterRelationship> relationships);

        public static native TemplateInstance storyNetwork(List<Character> characters, List<Location> locations,
                List<StoryEvent> recentEvents);

        public static native TemplateInstance locationConnections(Location location, List<Character> characters);

        public static native TemplateInstance sharedHistory(Character char1, Character char2, List<StoryEvent> events);
    }

    @Tool("""
            Create a new character in the story thread with optional tags.

            IMPORTANT: Choose meaningful names that will create good identifiers:
            - For named NPCs: use full names (e.g., "Thorin Oakenshield", "Gandalf the Grey")
            - For generic NPCs: include location or distinguishing feature (e.g., "Tavern Guard", "North Gate Guard", "Scarred Bartender")

            The name will be converted to a slug for the ID: "{storyThread}:{name-as-slug}"
            Example: "Thorin Oakenshield" → ID: "summer-adventure:thorin-oakenshield"

            Summary should be brief (5-10 words) for quick identification.
            Description can be detailed narrative and will evolve over time.

            Common tags:
            - Control: "player-controlled", "npc" (default if no tags specified)
            - Party: "companion", "temporary", "protagonist"
            - Roles: "quest-giver", "merchant", "informant", "villain", "mentor"
            - Status: "dead", "missing", "imprisoned", "retired"

            Tags parameter should be a list like: ["player-controlled", "protagonist"]
            """)
    public String createCharacter(String storyThreadId, String name, String summary, String description,
            List<String> tags) {
        Character character = storyRepository.createCharacter(storyThreadId, name, summary, description, tags);
        String tagInfo = tags != null && !tags.isEmpty() ? " with tags: " + String.join(", ", tags) : "";
        return "Created character: " + character.getName() + " (ID: " + character.getId() + ")" + tagInfo;
    }

    @Tool("""
            Update an existing character's basic information (name, summary, description, class, level).
            For alignment, use addCharacterTags with tags like "alignment:lawful-good".
            For other tag management, use addCharacterTags or removeCharacterTags.
            """)
    public String updateCharacter(String characterId, String name, String summary, String description,
            String characterClass, Integer level) {
        Character character = storyRepository.updateCharacter(characterId, name, summary, description,
                characterClass, level);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }
        return "Updated character: " + character.getName();
    }

    @Tool("""
            Add tags to a character. Tags are case-insensitive and automatically normalized.
            Common tags: "player-controlled", "companion", "quest-giver", "merchant", "villain", "dead", etc.
            Alignment tags: "alignment:lawful-good", "alignment:chaotic-neutral", "alignment:true-neutral", etc.
            Can also use prefixed tags like "faction:thieves-guild" or "profession:blacksmith".
            """)
    public String addCharacterTags(String characterId, List<String> tags) {
        Character character = storyRepository.addCharacterTags(characterId, tags);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }
        return "Added tags to " + character.getName() + ": " + String.join(", ", tags)
                + "\nCurrent tags: " + String.join(", ", character.getTags());
    }

    @Tool("""
            Remove tags from a character.
            """)
    public String removeCharacterTags(String characterId, List<String> tags) {
        Character character = storyRepository.removeCharacterTags(characterId, tags);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }
        return "Removed tags from " + character.getName() + ": " + String.join(", ", tags)
                + "\nCurrent tags: " + String.join(", ", character.getTags());
    }

    @Tool("""
            Find characters by tags. Returns characters that have ANY of the specified tags.
            For example: findCharactersByTags(storyThreadId, ["player-controlled", "companion"])
            will return all PCs and companions.
            """)
    public String findCharactersByTags(String storyThreadId, List<String> tags) {
        List<Character> characters = storyRepository.findCharactersByAnyTag(storyThreadId, tags);
        if (characters.isEmpty()) {
            return "No characters found with tags: " + String.join(", ", tags);
        }
        return Templates.characterList(characters).render();
    }

    @Tool("""
            Get detailed information about a specific character including relationships and recent events.
            """)
    public String getCharacterDetail(String characterId) {
        Character character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }
        return Templates.characterDetail(character).render();
    }

    @Tool("""
            Transfer control of a character between player and GM.
            Use this when a companion joins or leaves the party, or when converting an NPC to a PC.
            Control options: "player" or "gm"
            """)
    public String setCharacterControl(String characterId, String control) {
        Character character = storyRepository.setCharacterControl(characterId, control);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }
        boolean isPlayerControlled = character.hasTag("player-controlled");
        return "Character " + character.getName() + " is now " +
                (isPlayerControlled ? "player-controlled" : "GM-controlled");
    }

    @Tool("""
            Get all player characters (PCs) in the story thread.
            """)
    public String getPlayerCharacters(String storyThreadId) {
        List<Character> characters = storyRepository.getPlayerCharacters(storyThreadId);
        if (characters.isEmpty()) {
            return "No player characters found in this story.";
        }
        return Templates.characterList(characters).render();
    }

    @Tool("""
            Get all party members (PCs and companions) in the story thread.
            """)
    public String getPartyMembers(String storyThreadId) {
        List<Character> characters = storyRepository.getPartyMembers(storyThreadId);
        if (characters.isEmpty()) {
            return "No party members found in this story.";
        }
        return Templates.characterList(characters).render();
    }

    @Tool("""
            Get all characters in the story thread (not filtered by tags).
            Use this to see the complete cast of characters.
            """)
    public String getAllCharacters(String storyThreadId) {
        List<Character> characters = storyRepository.findCharactersByStoryThreadId(storyThreadId);
        if (characters.isEmpty()) {
            return "No characters found in this story.";
        }
        return Templates.characterList(characters).render();
    }

    // ===== LOCATION METHODS =====

    @Tool("""
            Create a new location in the story thread with optional tags.
            Location tags examples: "city", "dungeon", "wilderness", "tavern", "destroyed", "hidden", etc.
            """)
    public String createLocation(String storyThreadId, String name, String summary, String description,
            List<String> tags) {
        Location location = storyRepository.createLocation(storyThreadId, name, summary, description, tags);
        String tagInfo = tags != null && !tags.isEmpty() ? " with tags: " + String.join(", ", tags) : "";
        return "Created location: " + location.getName() + " (ID: " + location.getId() + ")" + tagInfo;
    }

    @Tool("""
            Update an existing location's basic information.
            """)
    public String updateLocation(String locationId, String name, String summary, String description) {
        Location location = storyRepository.updateLocation(locationId, name, summary, description);
        if (location == null) {
            return "Error: Location not found with ID: " + locationId;
        }
        return "Updated location: " + location.getName();
    }

    @Tool("""
            Add tags to a location.
            Common location tags: "city", "town", "dungeon", "wilderness", "tavern", "shop", "temple",
            "destroyed", "abandoned", "hidden", "fortified", "sacred", etc.
            """)
    public String addLocationTags(String locationId, List<String> tags) {
        Location location = storyRepository.addLocationTags(locationId, tags);
        if (location == null) {
            return "Error: Location not found with ID: " + locationId;
        }
        return "Added tags to " + location.getName() + ": " + String.join(", ", tags)
                + "\nCurrent tags: " + String.join(", ", location.getTags());
    }

    @Tool("""
            Remove tags from a location.
            """)
    public String removeLocationTags(String locationId, List<String> tags) {
        Location location = storyRepository.removeLocationTags(locationId, tags);
        if (location == null) {
            return "Error: Location not found with ID: " + locationId;
        }
        return "Removed tags from " + location.getName() + ": " + String.join(", ", tags)
                + "\nCurrent tags: " + String.join(", ", location.getTags());
    }

    @Tool("""
            Find locations by tags. Returns locations that have ANY of the specified tags.
            """)
    public String findLocationsByTags(String storyThreadId, List<String> tags) {
        List<Location> locations = storyRepository.findLocationsByAnyTag(storyThreadId, tags);
        if (locations.isEmpty()) {
            return "No locations found with tags: " + String.join(", ", tags);
        }
        return Templates.locationList(locations).render();
    }

    @Tool("""
            Get all locations in the story thread (not filtered by tags).
            Use this to see all known places in the story.
            """)
    public String getAllLocations(String storyThreadId) {
        List<Location> locations = storyRepository.findLocationsByStoryThreadId(storyThreadId);
        if (locations.isEmpty()) {
            return "No locations found in this story.";
        }
        return Templates.locationList(locations).render();
    }

    @Tool("""
            Get detailed information about a specific location.
            """)
    public String getLocationDetail(String locationId) {
        Location location = storyRepository.findLocationById(locationId);
        if (location == null) {
            return "Error: Location not found with ID: " + locationId;
        }
        return Templates.locationDetail(location).render();
    }

    @Tool("""
            Create a directional connection between two locations.
            Direction examples: "north", "south", "east", "west", "up", "down", "in", "through"
            Description example: "A winding forest path" or "Stone stairs leading down"
            """)
    public String connectLocations(String fromLocationId, String toLocationId, String direction, String description) {
        storyRepository.connectLocations(fromLocationId, toLocationId, direction, description);
        return "Connected locations with direction: " + direction;
    }

    // ===== STORY EVENT METHODS =====

    @Tool("""
            Create a new story event with optional participants and locations.
            Event tags examples: "combat", "social", "exploration", "quest-start", "character-death", etc.
            """)
    public String createEvent(String storyThreadId, String title, String description, Long day,
            List<String> participantIds, List<String> locationIds, List<String> tags) {
        StoryEvent event = storyRepository.createEvent(storyThreadId, title, description, day,
                participantIds, locationIds, tags);
        String tagInfo = tags != null && !tags.isEmpty() ? " with tags: " + String.join(", ", tags) : "";
        return "Created event: " + event.getTitle() + " (Day " + event.getDay() + ")" + tagInfo;
    }

    @Tool("""
            Find events by tags. Returns events that have ANY of the specified tags.
            """)
    public String findEventsByTags(String storyThreadId, List<String> tags) {
        List<StoryEvent> events = storyRepository.findEventsByAnyTag(storyThreadId, tags);
        if (events.isEmpty()) {
            return "No events found with tags: " + String.join(", ", tags);
        }
        StringBuilder result = new StringBuilder("Found " + events.size() + " events:\n");
        for (StoryEvent event : events) {
            result.append("- Day ").append(event.getDay()).append(": ").append(event.getTitle()).append("\n");
        }
        return result.toString();
    }

    @Tool("""
            Get recent events in the story thread.
            Limit parameter controls how many recent events to return (default 10, max 50).
            Use this to review what's happened recently in the story.
            """)
    public String getRecentEvents(String storyThreadId, Integer limit) {
        int eventLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
        List<StoryEvent> events = storyRepository.findRecentEvents(storyThreadId, eventLimit);

        if (events.isEmpty()) {
            return "No events found in this story.";
        }

        StringBuilder result = new StringBuilder("Found " + events.size() + " recent events:\n");
        for (StoryEvent event : events) {
            result.append("- Day ").append(event.getDay()).append(": ").append(event.getTitle());
            if (event.getTags() != null && !event.getTags().isEmpty()) {
                result.append(" [").append(String.join(", ", event.getTags())).append("]");
            }
            result.append("\n");
        }
        return result.toString();
    }

    // ===== RELATIONSHIP METHODS =====

    @Tool("""
            Create a relationship between two characters.
            Relationship tags examples: "friend", "enemy", "ally", "family", "mentor", "lover", "rival", etc.
            Multiple tags can express complex relationships: ["ally", "friend", "trusts"]
            """)
    public String createRelationship(String character1Id, String character2Id, List<String> tags) {
        storyRepository.createRelationship(character1Id, character2Id, tags);
        String tagInfo = tags != null && !tags.isEmpty() ? " (" + String.join(", ", tags) + ")" : "";
        return "Created relationship between characters" + tagInfo;
    }

    @Tool("""
            Get all relationships for a specific character.
            Returns formatted information about who this character is connected to and how.
            """)
    public String getCharacterRelationships(String characterId) {
        Character character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }

        List<CharacterRelationship> relationships = storyRepository.findRelationshipsByCharacterId(characterId);
        return Templates.characterRelationships(character, relationships).render();
    }

    // ===== QUERY METHODS =====

    @Tool("""
            Get a comprehensive network view of the story showing characters, locations, and their connections.
            This provides a high-level overview of the current story state.
            """)
    public String getStoryNetwork(String storyThreadId) {
        List<Character> characters = storyRepository.findCharactersByStoryThreadId(storyThreadId);
        List<Location> locations = storyRepository.findLocationsByStoryThreadId(storyThreadId);
        List<StoryEvent> recentEvents = storyRepository.findRecentEvents(storyThreadId, 5);

        return Templates.storyNetwork(characters, locations, recentEvents).render();
    }

    @Tool("""
            Get characters associated with a specific location.
            Useful for knowing who is present at a location or who has connections to it.
            """)
    public String getLocationConnections(String locationId) {
        Location location = storyRepository.findLocationById(locationId);
        if (location == null) {
            return "Error: Location not found with ID: " + locationId;
        }

        List<Character> characters = storyRepository.findCharactersByLocation(locationId);
        return Templates.locationConnections(location, characters).render();
    }

    @Tool("""
            Get shared history between two characters - events they both participated in.
            Useful for understanding their relationship context.
            """)
    public String getSharedHistory(String character1Id, String character2Id) {
        Character char1 = storyRepository.findCharacterById(character1Id);
        Character char2 = storyRepository.findCharacterById(character2Id);

        if (char1 == null || char2 == null) {
            return "Error: One or both characters not found";
        }

        List<StoryEvent> events = storyRepository.findSharedEvents(character1Id, character2Id);
        return Templates.sharedHistory(char1, char2, events).render();
    }
}
