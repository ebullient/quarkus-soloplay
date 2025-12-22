package dev.ebullient.soloplay.api;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.StoryTools;
import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.CharacterRelationship;
import dev.ebullient.soloplay.data.Location;
import dev.ebullient.soloplay.data.StoryEvent;
import dev.ebullient.soloplay.data.StoryThread;

/**
 * REST API for inspecting story data.
 * Provides JSON endpoints for programmatic access to characters, locations, events, and relationships.
 * Mirrors the /inspect web UI routes but returns JSON instead of HTML.
 */
@ApplicationScoped
@Path("/api/inspect")
public class InspectResource {

    @Inject
    StoryRepository storyRepository;

    @Inject
    StoryTools storyTools;

    /**
     * Get list of all story threads.
     */
    @GET
    @Path("/threads")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StoryThread> getStoryThreads() {
        return storyRepository.findAllStoryThreads();
    }

    /**
     * Get a specific story thread by ID.
     */
    @GET
    @Path("/story/{threadId}")
    @Produces(MediaType.APPLICATION_JSON)
    public StoryThread getStoryThread(@PathParam("threadId") String threadId) {
        return storyRepository.findStoryThreadBySlug(threadId);
    }

    /**
     * Get all characters in a story thread.
     */
    @GET
    @Path("/story/{threadId}/characters")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Character> getCharacters(@PathParam("threadId") String threadId) {
        return storyRepository.findCharactersByStoryThreadId(threadId);
    }

    /**
     * Get a specific character.
     */
    @GET
    @Path("/story/{threadId}/characters/{characterSlug}")
    @Produces(MediaType.APPLICATION_JSON)
    public Character getCharacter(@PathParam("threadId") String threadId,
            @PathParam("characterSlug") String characterSlug) {
        String characterId = threadId + ":" + characterSlug;
        return storyRepository.findCharacterById(characterId);
    }

    /**
     * Get all locations in a story thread.
     */
    @GET
    @Path("/story/{threadId}/locations")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Location> getLocations(@PathParam("threadId") String threadId) {
        return storyRepository.findLocationsByStoryThreadId(threadId);
    }

    /**
     * Get a specific location.
     */
    @GET
    @Path("/story/{threadId}/locations/{locationSlug}")
    @Produces(MediaType.APPLICATION_JSON)
    public Location getLocation(@PathParam("threadId") String threadId,
            @PathParam("locationSlug") String locationSlug) {
        String locationId = threadId + ":" + locationSlug;
        return storyRepository.findLocationById(locationId);
    }

    /**
     * Get all relationships in a story thread.
     */
    @GET
    @Path("/story/{threadId}/relationships")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CharacterRelationship> getRelationships(@PathParam("threadId") String threadId) {
        return storyRepository.findRelationshipsByStoryThreadId(threadId);
    }

    /**
     * Get relationships for a specific character.
     */
    @GET
    @Path("/story/{threadId}/characters/{characterSlug}/relationships")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CharacterRelationship> getCharacterRelationships(@PathParam("threadId") String threadId,
            @PathParam("characterSlug") String characterSlug) {
        String characterId = threadId + ":" + characterSlug;
        return storyRepository.findRelationshipsByCharacterId(characterId);
    }

    /**
     * Get characters connected to a location.
     */
    @GET
    @Path("/story/{threadId}/locations/{locationSlug}/connections")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Character> getLocationConnections(@PathParam("threadId") String threadId,
            @PathParam("locationSlug") String locationSlug) {
        String locationId = threadId + ":" + locationSlug;
        return storyRepository.findCharactersByLocation(locationId);
    }

    /**
     * Get shared history between two characters.
     */
    @GET
    @Path("/story/{threadId}/shared-history/{char1Slug}/{char2Slug}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StoryEvent> getSharedHistory(@PathParam("threadId") String threadId,
            @PathParam("char1Slug") String char1Slug,
            @PathParam("char2Slug") String char2Slug) {
        String char1Id = threadId + ":" + char1Slug;
        String char2Id = threadId + ":" + char2Slug;
        return storyRepository.findSharedEvents(char1Id, char2Id);
    }

    // === AI TOOL OUTPUT INSPECTION (as text) ===

    /**
     * Get AI tool output for character relationships.
     * Returns the formatted text that AI tools see.
     */
    @GET
    @Path("/story/{threadId}/ai/characters/{characterSlug}/relationships")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAiCharacterRelationships(@PathParam("threadId") String threadId,
            @PathParam("characterSlug") String characterSlug) {
        String characterId = threadId + ":" + characterSlug;
        return storyTools.getCharacterRelationships(characterId);
    }

    /**
     * Get AI tool output for story network.
     * Returns the formatted text that AI tools see.
     */
    @GET
    @Path("/story/{threadId}/ai/network")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAiStoryNetwork(@PathParam("threadId") String threadId) {
        return storyTools.getStoryNetwork(threadId);
    }

    /**
     * Get AI tool output for location connections.
     * Returns the formatted text that AI tools see.
     */
    @GET
    @Path("/story/{threadId}/ai/locations/{locationSlug}/connections")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAiLocationConnections(@PathParam("threadId") String threadId,
            @PathParam("locationSlug") String locationSlug) {
        String locationId = threadId + ":" + locationSlug;
        return storyTools.getLocationConnections(locationId);
    }

    /**
     * Get AI tool output for shared history.
     * Returns the formatted text that AI tools see.
     */
    @GET
    @Path("/story/{threadId}/ai/shared-history/{char1Slug}/{char2Slug}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAiSharedHistory(@PathParam("threadId") String threadId,
            @PathParam("char1Slug") String char1Slug,
            @PathParam("char2Slug") String char2Slug) {
        String char1Id = threadId + ":" + char1Slug;
        String char2Id = threadId + ":" + char2Slug;
        return storyTools.getSharedHistory(char1Id, char2Id);
    }
}
