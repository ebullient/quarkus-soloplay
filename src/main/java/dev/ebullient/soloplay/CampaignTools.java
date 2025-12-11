package dev.ebullient.soloplay;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.data.CampaignEvent;
import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.CharacterRelationship;
import dev.ebullient.soloplay.data.Location;
import dev.langchain4j.agent.tool.Tool;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

/**
 * AI Tools for campaign data operations.
 * Delegates to CampaignRepository for actual data access.
 */
@ApplicationScoped
public class CampaignTools {

    @Inject
    CampaignRepository campaignRepository;

    @CheckedTemplate(basePath = "tools")
    public static class Templates {
        public static native TemplateInstance characterDetail(Character character);

        public static native TemplateInstance characterList(List<Character> characters);

        public static native TemplateInstance locationDetail(Location location);

        public static native TemplateInstance locationList(List<Location> locations);
    }

    @Tool("""
            Create a new character (PC, NPC, or SIDEKICK) in the campaign.
            Summary should be brief (5-10 words) for quick identification.
            Description can be detailed narrative.
            """)
    public String createCharacter(String campaignId, String type, String name, String summary, String description) {
        Character character = campaignRepository.createCharacter(campaignId, type, name, summary, description);
        return "Created character: " + character.getName() + " (ID: " + character.getId() + ")";
    }

    @Tool("""
            Update an existing character's information.
            All fields except ID can be updated.
            """)
    public String updateCharacter(String characterId, String name, String summary, String description,
            String characterClass, Integer level, String alignment, String status) {
        Character character = campaignRepository.updateCharacter(characterId, name, summary, description,
                characterClass, level, alignment, status);
        if (character == null) {
            return "Error: Character not found with ID: " + characterId;
        }
        return "Updated character: " + character.getName();
    }

    @Tool("List all characters in a campaign")
    public String listCharacters(String campaignId) {
        List<Character> characters = campaignRepository.findCharactersByCampaignId(campaignId);

        if (characters.isEmpty()) {
            return "No characters found in campaign";
        }
        return Templates.characterList(characters).render();
    }

    @Tool("Find characters by name (partial match, may return multiple results)")
    public String findCharacter(String campaignId, String name) {
        List<Character> characters = campaignRepository.findCharactersByNameContaining(campaignId, name);

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
        Character character = campaignRepository.findCharacterById(characterId);
        if (character == null) {
            return "Character not found with ID: " + characterId;
        }
        return Templates.characterDetail(character).render();
    }

    @Tool("""
            Create a new location (CITY, REGION, BUILDING, DUNGEON, etc.) in the campaign.
            Summary should be brief (5-10 words) for quick identification.
            Description can be detailed narrative.
            """)
    public String createLocation(String campaignId, String type, String name, String summary, String description) {
        Location location = campaignRepository.createLocation(campaignId, type, name, summary, description);
        return "Created location: " + location.getName() + " (ID: " + location.getId() + ")";
    }

    @Tool("""
            Update an existing location's information.
            All fields except ID can be updated.
            """)
    public String updateLocation(String locationId, String name, String summary, String description) {
        Location location = campaignRepository.updateLocation(locationId, name, summary, description);
        if (location == null) {
            return "Error: Location not found with ID: " + locationId;
        }
        return "Updated location: " + location.getName();
    }

    @Tool("List all locations in a campaign")
    public String listLocations(String campaignId) {
        List<Location> locations = campaignRepository.findLocationsByCampaignId(campaignId);

        if (locations.isEmpty()) {
            return "No locations found in campaign";
        }
        return Templates.locationList(locations).render();
    }

    @Tool("Find locations by name (partial match, may return multiple results)")
    public String findLocation(String campaignId, String name) {
        List<Location> locations = campaignRepository.findLocationsByNameContaining(campaignId, name);

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
        Location location = campaignRepository.findLocationById(locationId);
        if (location == null) {
            return "Location not found with ID: " + locationId;
        }
        return Templates.locationDetail(location).render();
    }

    @Tool("Create a relationship between two characters")
    public String createRelationship(String fromCharacterId, String toCharacterId,
            String relationshipType, String description) {
        var relationship = campaignRepository.createRelationship(fromCharacterId, toCharacterId, relationshipType,
                description);
        if (relationship == null) {
            return "Error: Could not create relationship (check that both character IDs exist)";
        }
        return "Created relationship between characters";
    }

    @Tool("Find all relationships for a character - shows who they know and how")
    public String getCharacterRelationships(String characterId) {
        List<CharacterRelationship> relationships = campaignRepository.findRelationshipsByCharacterId(characterId);
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

    @Tool("Get the relationship network for a campaign - shows all character connections")
    public String getCampaignNetwork(String campaignId) {
        List<CharacterRelationship> relationships = campaignRepository.findRelationshipsByCampaignId(campaignId);
        if (relationships.isEmpty()) {
            return "No relationships found in campaign: " + campaignId;
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
        List<Character> characters = campaignRepository.findCharactersByLocation(locationId);
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
        List<CampaignEvent> events = campaignRepository.findSharedEvents(character1Id, character2Id);
        if (events.isEmpty()) {
            return "No shared events found between these characters.";
        }

        StringBuilder result = new StringBuilder();
        result.append("Shared events:\n");
        for (CampaignEvent event : events) {
            result.append(String.format("- %s", event.getDescription()));
            if (event.getLocation() != null) {
                result.append(String.format(" (at %s)", event.getLocation().getName()));
            }
            result.append("\n");
        }
        return result.toString();
    }

    @Tool("List all campaign IDs that have data")
    public List<String> getCampaignIds() {
        return campaignRepository.getCampaignIds();
    }
}