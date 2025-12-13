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
    }

    @Tool("""
            Create a new character in the story thread with optional tags.
            Summary should be brief (5-10 words) for quick identification.
            Description can be detailed narrative.

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
            Update an existing character's basic information (name, summary, description, class, level, alignment).
            For tag management, use addCharacterTags or removeCharacterTags instead.
            """)
    public String updateCharacter(String characterId, String name, String summary, String description,
            String characterClass, Integer level, String alignment) {
        Character character = storyRepository.updateCharacter(characterId, name, summary, description,
                characterClass, level, alignment);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }
        return "Updated character: " + character.getName();
    }

    @Tool("""
            Add tags to a character. Tags are case-insensitive and automatically normalized.
            Common tags: "player-controlled", "companion", "quest-giver", "merchant", "villain", "dead", etc.
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

    @Tool("List all characters in a story thread")
    public String listCharacters(String storyThreadId) {
        List<Character> characters = storyRepository.findCharactersByStoryThreadId(storyThreadId);

        if (characters.isEmpty()) {
            return "No characters found in story thread";
        }
        return Templates.characterList(characters).render();
    }

    @Tool("Find characters by name (partial match, may return multiple results)")
    public String findCharacter(String storyThreadId, String name) {
        List<Character> characters = storyRepository.findCharactersByNameContaining(storyThreadId, name);

        if (characters.isEmpty()) {
            return "No character found with name containing: " + name;
        }

        if (characters.size() == 1) {
            return Templates.characterDetail(characters.get(0)).render();
        } else {
            return Templates.characterList(characters).render();
        }
    }

    @Tool("Get detailed information about a specific character by ID")
    public String getCharacterDetail(String characterId) {
        Character character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            return "Character not found with ID: " + characterId;
        }
        return Templates.characterDetail(character).render();
    }

    @Tool("""
            Find characters that have ANY of the specified tags (OR logic).
            Example: findCharactersByTags(threadId, ["merchant", "quest-giver"]) finds merchants OR quest-givers.
            """)
    public String findCharactersByTags(String storyThreadId, List<String> tags) {
        List<Character> characters = storyRepository.findCharactersByAnyTag(storyThreadId, tags);
        if (characters.isEmpty()) {
            return "No characters found with tags: " + String.join(", ", tags);
        }
        return Templates.characterList(characters).render();
    }

    @Tool("""
            Get all player-controlled characters (PCs) in the story thread.
            """)
    public String getPlayerCharacters(String storyThreadId) {
        List<Character> characters = storyRepository.findCharactersByAnyTag(storyThreadId,
                List.of("player-controlled"));
        if (characters.isEmpty()) {
            return "No player-controlled characters found";
        }
        return Templates.characterList(characters).render();
    }

    @Tool("""
            Get all party members (player-controlled characters and companions).
            """)
    public String getPartyMembers(String storyThreadId) {
        List<Character> characters = storyRepository.findCharactersByAnyTag(storyThreadId,
                List.of("player-controlled", "companion"));
        if (characters.isEmpty()) {
            return "No party members found";
        }
        return Templates.characterList(characters).render();
    }

    @Tool("""
            Add a character to the party temporarily (adds "temporary" tag).
            Useful for NPCs joining for combat or a single quest.
            """)
    public String addTemporaryPartyMember(String characterId) {
        return addCharacterTags(characterId, List.of("temporary", "companion"));
    }

    @Tool("""
            Remove a character from temporary party status (removes "temporary" and "companion" tags).
            """)
    public String removeTemporaryPartyMember(String characterId) {
        return removeCharacterTags(characterId, List.of("temporary", "companion"));
    }

    @Tool("""
            Transfer character control between player and GM.
            If playerControlled is true, removes "npc" and adds "player-controlled".
            If false, removes "player-controlled" and adds "npc".
            """)
    public String setCharacterControl(String characterId, boolean playerControlled) {
        Character character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }

        if (playerControlled) {
            storyRepository.removeCharacterTags(characterId, List.of("npc"));
            character = storyRepository.addCharacterTags(characterId, List.of("player-controlled"));
            return character.getName() + " is now player-controlled";
        } else {
            storyRepository.removeCharacterTags(characterId, List.of("player-controlled"));
            character = storyRepository.addCharacterTags(characterId, List.of("npc"));
            return character.getName() + " is now GM-controlled (NPC)";
        }
    }

    @Tool("""
            Get all quest-givers in the story thread.
            """)
    public String getQuestGivers(String storyThreadId) {
        List<Character> characters = storyRepository.findCharactersByAnyTag(storyThreadId,
                List.of("quest-giver"));
        if (characters.isEmpty()) {
            return "No quest-givers found";
        }
        return Templates.characterList(characters).render();
    }

    @Tool("""
            Create a new location (CITY, REGION, BUILDING, DUNGEON, etc.) in the story thread.
            Summary should be brief (5-10 words) for quick identification.
            Description can be detailed narrative.
            """)
    public String createLocation(String storyThreadId, String type, String name, String summary, String description) {
        Location location = storyRepository.createLocation(storyThreadId, type, name, summary, description);
        return "Created location: " + location.getName() + " (ID: " + location.getId() + ")";
    }

    @Tool("""
            Update an existing location's information.
            All fields except ID can be updated.
            """)
    public String updateLocation(String locationId, String name, String summary, String description) {
        Location location = storyRepository.updateLocation(locationId, name, summary, description);
        if (location == null) {
            return "Error: Location not found with ID: " + locationId;
        }
        return "Updated location: " + location.getName();
    }

    @Tool("List all locations in a story thread")
    public String listLocations(String storyThreadId) {
        List<Location> locations = storyRepository.findLocationsByStoryThreadId(storyThreadId);

        if (locations.isEmpty()) {
            return "No locations found in story thread";
        }
        return Templates.locationList(locations).render();
    }

    @Tool("Find locations by name (partial match, may return multiple results)")
    public String findLocation(String storyThreadId, String name) {
        List<Location> locations = storyRepository.findLocationsByNameContaining(storyThreadId, name);

        if (locations.isEmpty()) {
            return "No location found with name containing: " + name;
        }

        if (locations.size() == 1) {
            return Templates.locationDetail(locations.get(0)).render();
        } else {
            return Templates.locationList(locations).render();
        }
    }

    @Tool("Get detailed information about a specific location by ID")
    public String getLocationDetail(String locationId) {
        Location location = storyRepository.findLocationById(locationId);
        if (location == null) {
            return "Location not found with ID: " + locationId;
        }
        return Templates.locationDetail(location).render();
    }

    @Tool("Create a relationship between two characters")
    public String createRelationship(String fromCharacterId, String toCharacterId,
            String relationshipType, String description) {
        var relationship = storyRepository.createRelationship(fromCharacterId, toCharacterId, relationshipType,
                description);
        if (relationship == null) {
            return "Error: Could not create relationship (check that both character IDs exist)";
        }
        return "Created relationship between characters";
    }

    @Tool("Find all relationships for a character - shows who they know and how")
    public String getCharacterRelationships(String characterId) {
        List<CharacterRelationship> relationships = storyRepository.findRelationshipsByCharacterId(characterId);
        if (relationships.isEmpty()) {
            return "No relationships found for character: " + characterId;
        }

        StringBuilder result = new StringBuilder();
        result.append("Relationships for character:\n");
        for (CharacterRelationship rel : relationships) {
            String fromName = rel.getFrom().getName();
            String toName = rel.getTo().getName();
            String type = rel.getType().toString();
            String desc = rel.getDescription() != null ? rel.getDescription() : "";

            result.append(String.format("- %s %s %s", fromName, type, toName));
            if (!desc.isEmpty()) {
                result.append(String.format(" (%s)", desc));
            }
            result.append("\n");
        }
        return result.toString();
    }

    @Tool("Get the relationship network for a story thread - shows all character connections")
    public String getStoryNetwork(String storyThreadId) {
        List<CharacterRelationship> relationships = storyRepository.findRelationshipsByStoryThreadId(storyThreadId);
        if (relationships.isEmpty()) {
            return "No relationships found in story thread: " + storyThreadId;
        }

        StringBuilder result = new StringBuilder();
        result.append("Character relationship network:\n");
        for (CharacterRelationship rel : relationships) {
            String fromName = rel.getFrom().getName();
            String toName = rel.getTo().getName();
            String type = rel.getType().toString();
            result.append(String.format("- %s %s %s\n", fromName, type, toName));
        }
        return result.toString();
    }

    @Tool("Find characters connected to a location - who has been there")
    public String getLocationConnections(String locationId) {
        List<Character> characters = storyRepository.findCharactersByLocation(locationId);
        if (characters.isEmpty()) {
            return "No characters found connected to location: " + locationId;
        }

        StringBuilder result = new StringBuilder();
        result.append("Characters connected to this location:\n");
        for (Character character : characters) {
            result.append(String.format("- %s (%s)\n", character.getName(),
                    character.getSummary() != null ? character.getSummary() : "no description"));
        }
        return result.toString();
    }

    @Tool("Find shared history between two characters - events they both participated in")
    public String getSharedHistory(String character1Id, String character2Id) {
        List<StoryEvent> events = storyRepository.findSharedEvents(character1Id, character2Id);
        if (events.isEmpty()) {
            return "No shared events found between these characters.";
        }

        StringBuilder result = new StringBuilder();
        result.append("Shared events:\n");
        for (StoryEvent event : events) {
            result.append(String.format("- %s", event.getDescription()));
            if (event.getLocation() != null) {
                result.append(String.format(" (at %s)", event.getLocation().getName()));
            }
            result.append("\n");
        }
        return result.toString();
    }

    @Tool("List all story thread IDs that have data")
    public List<String> getStoryThreadIds() {
        return storyRepository.getStoryThreadIds();
    }
}
