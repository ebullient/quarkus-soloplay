package dev.ebullient.soloplay.web;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.StoryTools;
import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.CharacterRelationship;
import dev.ebullient.soloplay.data.Location;
import dev.ebullient.soloplay.data.StoryEvent;
import dev.ebullient.soloplay.data.StoryThread;
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
public class Inspect extends Controller {

    @Inject
    StoryRepository storyRepository;

    @Inject
    StoryTools storyTools;

    @CheckedTemplate
    public static class Templates {
        // Story thread selection
        public static native TemplateInstance selectThread(List<StoryThread> threads);

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
     * Story thread selection page.
     * Shows list of available story threads to inspect.
     */
    @GET
    @Path("/")
    public TemplateInstance selectThread() {
        List<StoryThread> threads = storyRepository.findAllStoryThreads();
        return Templates.selectThread(threads);
    }

    /**
     * Main story data dashboard for a specific thread.
     * Shows overview of all story elements.
     */
    @GET
    @Path("/story/{threadId}")
    public TemplateInstance index(@RestPath String threadId) {
        return Templates.index(threadId);
    }

    /**
     * List all characters in a story thread.
     */
    @GET
    @Path("/story/{threadId}/characters")
    public TemplateInstance characters(@RestPath String threadId) {
        List<Character> characters = storyRepository.findCharactersByStoryThreadId(threadId);
        return Templates.characters(characters, threadId);
    }

    /**
     * View a single character's details.
     */
    @GET
    @Path("/story/{threadId}/characters/{characterSlug}")
    public TemplateInstance characterDetail(@RestPath String threadId, @RestPath String characterSlug) {
        String characterId = threadId + ":" + characterSlug;
        Character character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            notFound(); // throws w/ 404
        }
        return Templates.characterDetail(character);
    }

    /**
     * List all locations in a story thread.
     */
    @GET
    @Path("/story/{threadId}/locations")
    public TemplateInstance locations(@RestPath String threadId) {
        List<Location> locations = storyRepository.findLocationsByStoryThreadId(threadId);
        return Templates.locations(locations, threadId);
    }

    /**
     * View a single location's details.
     */
    @GET
    @Path("/story/{threadId}/locations/{locationSlug}")
    public TemplateInstance locationDetail(@RestPath String threadId, @RestPath String locationSlug) {
        String locationId = threadId + ":" + locationSlug;
        Location location = storyRepository.findLocationById(locationId);
        if (location == null) {
            notFound(); // throws w/ 404
        }
        return Templates.locationDetail(location);
    }

    /**
     * View all relationships in a story thread (relationship network).
     */
    @GET
    @Path("/story/{threadId}/relationships")
    public TemplateInstance relationships(@RestPath String threadId) {
        List<CharacterRelationship> relationships = storyRepository.findRelationshipsByStoryThreadId(threadId);
        return Templates.relationships(threadId, relationships);
    }

    /**
     * View relationships for a specific character.
     */
    @GET
    @Path("/story/{threadId}/characters/{characterSlug}/relationships")
    public TemplateInstance characterRelationships(@RestPath String threadId, @RestPath String characterSlug) {
        String characterId = threadId + ":" + characterSlug;
        Character character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            notFound(); // throws w/ 404
        }
        List<CharacterRelationship> relationships = storyRepository.findRelationshipsByCharacterId(characterId);
        return Templates.characterRelationships(character, relationships);
    }

    /**
     * View characters connected to a location.
     */
    @GET
    @Path("/story/{threadId}/locations/{locationSlug}/connections")
    public TemplateInstance locationConnections(@RestPath String threadId, @RestPath String locationSlug) {
        String locationId = threadId + ":" + locationSlug;
        Location location = storyRepository.findLocationById(locationId);
        if (location == null) {
            notFound(); // throws w/ 404
        }
        List<Character> connectedCharacters = storyRepository.findCharactersByLocation(locationId);
        return Templates.locationConnections(location, connectedCharacters);
    }

    /**
     * View shared history between two characters.
     */
    @GET
    @Path("/story/{threadId}/shared-history/{char1Slug}/{char2Slug}")
    public TemplateInstance sharedHistory(@RestPath String threadId, @RestPath String char1Slug,
            @RestPath String char2Slug) {
        String char1Id = threadId + ":" + char1Slug;
        String char2Id = threadId + ":" + char2Slug;
        Character char1 = storyRepository.findCharacterById(char1Id);
        Character char2 = storyRepository.findCharacterById(char2Id);
        if (char1 == null || char2 == null) {
            notFound(); // throws w/ 404
        }
        List<StoryEvent> sharedEvents = storyRepository.findSharedEvents(char1Id, char2Id);
        return Templates.sharedHistory(char1, char2, sharedEvents);
    }

    // === AI TOOL OUTPUT INSPECTION ===

    /**
     * See what the AI tool returns for character relationships.
     */
    @GET
    @Path("/story/{threadId}/ai/characters/{characterSlug}/relationships")
    public TemplateInstance aiCharacterRelationships(@RestPath String threadId, @RestPath String characterSlug) {
        String characterId = threadId + ":" + characterSlug;
        String output = storyTools.getCharacterRelationships(characterId);
        return Templates.aiToolOutput("Character Relationships", output);
    }

    /**
     * See what the AI tool returns for story network.
     */
    @GET
    @Path("/story/{threadId}/ai/network")
    public TemplateInstance aiStoryNetwork(@RestPath String threadId) {
        String output = storyTools.getStoryNetwork(threadId);
        return Templates.aiToolOutput("Story Network", output);
    }

    /**
     * See what the AI tool returns for location connections.
     */
    @GET
    @Path("/story/{threadId}/ai/locations/{locationSlug}/connections")
    public TemplateInstance aiLocationConnections(@RestPath String threadId, @RestPath String locationSlug) {
        String locationId = threadId + ":" + locationSlug;
        String output = storyTools.getLocationConnections(locationId);
        return Templates.aiToolOutput("Location Connections", output);
    }

    /**
     * See what the AI tool returns for shared history.
     */
    @GET
    @Path("/story/{threadId}/ai/shared-history/{char1Slug}/{char2Slug}")
    public TemplateInstance aiSharedHistory(@RestPath String threadId, @RestPath String char1Slug,
            @RestPath String char2Slug) {
        String char1Id = threadId + ":" + char1Slug;
        String char2Id = threadId + ":" + char2Slug;
        String output = storyTools.getSharedHistory(char1Id, char2Id);
        return Templates.aiToolOutput("Shared History", output);
    }
}
