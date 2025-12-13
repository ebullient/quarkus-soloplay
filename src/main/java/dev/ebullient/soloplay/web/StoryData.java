package dev.ebullient.soloplay.web;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.StoryTools;
import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.CharacterRelationship;
import dev.ebullient.soloplay.data.Location;
import dev.ebullient.soloplay.data.StoryEvent;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

/**
 * Controller for viewing and managing story data.
 * Provides "Inspect" interface - CRUD views over characters, locations, events, and relationships.
 * This allows us to oversee what the AI is working with in the story database,
 * but.. SPOILERS!
 */
@Path("/inspect")
public class StoryData extends Controller {

    @Inject
    StoryRepository storyRepository;

    @Inject
    StoryTools storyTools;

    @CheckedTemplate
    public static class Templates {
        // Main dashboard
        public static native TemplateInstance index(String storyThreadId);

        // Character views
        public static native TemplateInstance characters(List<Character> characters, String storyThreadId);

        public static native TemplateInstance characterDetail(Character character);

        // Location views
        public static native TemplateInstance locations(List<Location> locations, String storyThreadId);

        public static native TemplateInstance locationDetail(Location location);

        // Relationship views
        public static native TemplateInstance relationships(String storyThreadId, List<CharacterRelationship> relationships);

        public static native TemplateInstance characterRelationships(Character character,
                List<CharacterRelationship> relationships);

        public static native TemplateInstance locationConnections(Location location, List<Character> connectedCharacters);

        public static native TemplateInstance sharedHistory(Character char1, Character char2, List<StoryEvent> sharedEvents);

        // AI Tool outputs (what the AI sees)
        public static native TemplateInstance aiToolOutput(String toolName, String output);
    }

    /**
     * Main story data dashboard.
     * Shows overview of all story elements.
     * Requires storyThreadId to be provided as query parameter.
     */
    @GET
    @Path("/")
    public TemplateInstance index(@RestQuery String storyThreadId) {
        if (storyThreadId == null || storyThreadId.isBlank()) {
            validation.addError("storyThreadId", "Story Thread ID is required");
            // Return error page or redirect to story thread selection
            throw new IllegalArgumentException("storyThreadId parameter is required");
        }
        return Templates.index(storyThreadId);
    }

    /**
     * List all characters in a story thread.
     * Requires storyThreadId to be provided as query parameter.
     */
    @GET
    @Path("/characters")
    public TemplateInstance characters(@RestQuery String storyThreadId) {
        if (storyThreadId == null || storyThreadId.isBlank()) {
            throw new IllegalArgumentException("storyThreadId parameter is required");
        }
        List<Character> characters = storyRepository.findCharactersByStoryThreadId(storyThreadId);
        return Templates.characters(characters, storyThreadId);
    }

    /**
     * View a single character's details.
     */
    @GET
    @Path("/characters/{id}")
    public TemplateInstance characterDetail(@RestPath String id) {
        Character character = storyRepository.findCharacterById(id);
        if (character == null) {
            notFound();
            return null; // This line is never reached, but needed for compilation
        }
        return Templates.characterDetail(character);
    }

    /**
     * List all locations in a story thread.
     * Requires storyThreadId to be provided as query parameter.
     */
    @GET
    @Path("/locations")
    public TemplateInstance locations(@RestQuery String storyThreadId) {
        if (storyThreadId == null || storyThreadId.isBlank()) {
            throw new IllegalArgumentException("storyThreadId parameter is required");
        }
        List<Location> locations = storyRepository.findLocationsByStoryThreadId(storyThreadId);
        return Templates.locations(locations, storyThreadId);
    }

    /**
     * View a single location's details.
     */
    @GET
    @Path("/locations/{id}")
    public TemplateInstance locationDetail(@RestPath String id) {
        Location location = storyRepository.findLocationById(id);
        if (location == null) {
            notFound();
            return null; // This line is never reached, but needed for compilation
        }
        return Templates.locationDetail(location);
    }

    /**
     * View all relationships in a story thread (relationship network).
     * Requires storyThreadId to be provided as query parameter.
     */
    @GET
    @Path("/relationships")
    public TemplateInstance relationships(@RestQuery String storyThreadId) {
        if (storyThreadId == null || storyThreadId.isBlank()) {
            throw new IllegalArgumentException("storyThreadId parameter is required");
        }
        List<CharacterRelationship> relationships = storyRepository.findRelationshipsByStoryThreadId(storyThreadId);
        return Templates.relationships(storyThreadId, relationships);
    }

    /**
     * View relationships for a specific character.
     */
    @GET
    @Path("/characters/{id}/relationships")
    public TemplateInstance characterRelationships(@RestPath String id) {
        Character character = storyRepository.findCharacterById(id);
        if (character == null) {
            notFound();
            return null;
        }
        List<CharacterRelationship> relationships = storyRepository.findRelationshipsByCharacterId(id);
        return Templates.characterRelationships(character, relationships);
    }

    /**
     * View characters connected to a location.
     */
    @GET
    @Path("/locations/{id}/connections")
    public TemplateInstance locationConnections(@RestPath String id) {
        Location location = storyRepository.findLocationById(id);
        if (location == null) {
            notFound();
            return null;
        }
        List<Character> connectedCharacters = storyRepository.findCharactersByLocation(id);
        return Templates.locationConnections(location, connectedCharacters);
    }

    /**
     * View shared history between two characters.
     */
    @GET
    @Path("/shared-history")
    public TemplateInstance sharedHistory(@RestQuery String char1Id, @RestQuery String char2Id) {
        if (char1Id == null || char2Id == null) {
            badRequest();
            return null;
        }
        Character char1 = storyRepository.findCharacterById(char1Id);
        Character char2 = storyRepository.findCharacterById(char2Id);
        if (char1 == null || char2 == null) {
            notFound();
            return null;
        }
        List<StoryEvent> sharedEvents = storyRepository.findSharedEvents(char1Id, char2Id);
        return Templates.sharedHistory(char1, char2, sharedEvents);
    }

    // === AI TOOL OUTPUT INSPECTION ===

    /**
     * See what the AI tool returns for character relationships.
     */
    @GET
    @Path("/ai/character-relationships/{id}")
    public TemplateInstance aiCharacterRelationships(@RestPath String id) {
        String output = storyTools.getCharacterRelationships(id);
        return Templates.aiToolOutput("Character Relationships", output);
    }

    /**
     * See what the AI tool returns for story network.
     * Requires storyThreadId to be provided as query parameter.
     */
    @GET
    @Path("/ai/story-network")
    public TemplateInstance aiStoryNetwork(@RestQuery String storyThreadId) {
        if (storyThreadId == null || storyThreadId.isBlank()) {
            throw new IllegalArgumentException("storyThreadId parameter is required");
        }
        String output = storyTools.getStoryNetwork(storyThreadId);
        return Templates.aiToolOutput("Story Network", output);
    }

    /**
     * See what the AI tool returns for location connections.
     */
    @GET
    @Path("/ai/location-connections/{id}")
    public TemplateInstance aiLocationConnections(@RestPath String id) {
        String output = storyTools.getLocationConnections(id);
        return Templates.aiToolOutput("Location Connections", output);
    }

    /**
     * See what the AI tool returns for shared history.
     */
    @GET
    @Path("/ai/shared-history")
    public TemplateInstance aiSharedHistory(@RestQuery String char1Id, @RestQuery String char2Id) {
        if (char1Id == null || char2Id == null) {
            badRequest();
            return null;
        }
        String output = storyTools.getSharedHistory(char1Id, char2Id);
        return Templates.aiToolOutput("Shared History", output);
    }
}
