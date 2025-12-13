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
            Create a new character (PC, NPC, or SIDEKICK) in the story thread.
            Summary should be brief (5-10 words) for quick identification.
            Description can be detailed narrative.
            """)
    public String createCharacter(String storyThreadId, String type, String name, String summary, String description) {
        Character character = storyRepository.createCharacter(storyThreadId, type, name, summary, description);
        return "Created character: " + character.getName() + " (ID: " + character.getId() + ")";
    }

    @Tool("""
            Update an existing character's information.
            All fields except ID can be updated.
            """)
    public String updateCharacter(String characterId, String name, String summary, String description,
            String characterClass, Integer level, String alignment, String status) {
        Character character = storyRepository.updateCharacter(characterId, name, summary, description,
                characterClass, level, alignment, status);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }
        return "Updated character: " + character.getName();
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
