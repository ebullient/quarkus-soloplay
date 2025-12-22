package dev.ebullient.soloplay.api;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.StoryTools;
import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.CharacterRelationship;
import dev.ebullient.soloplay.data.Location;
import dev.ebullient.soloplay.data.StoryEvent;
import dev.ebullient.soloplay.data.StoryThread;
import io.quarkus.logging.Log;

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
    public Response getStoryThread(@PathParam("threadId") String threadId) {
        var result = storyRepository.findStoryThreadBySlug(threadId);
        return result == null
                ? Response.status(Status.NOT_FOUND).build()
                : Response.ok(result).build();
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
    public Response getCharacter(@PathParam("threadId") String threadId,
            @PathParam("characterSlug") String characterSlug) {
        String characterId = threadId + ":" + characterSlug;
        var result = storyRepository.findCharacterById(characterId);
        return result == null
                ? Response.status(Status.NOT_FOUND).build()
                : Response.ok(result).build();
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
    public Response getLocation(@PathParam("threadId") String threadId,
            @PathParam("locationSlug") String locationSlug) {
        String locationId = threadId + ":" + locationSlug;
        var result = storyRepository.findLocationById(locationId);
        return result == null
                ? Response.status(Status.NOT_FOUND).build()
                : Response.ok(result).build();
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
    public Response getCharacterRelationships(@PathParam("threadId") String threadId,
            @PathParam("characterSlug") String characterSlug) {
        String characterId = threadId + ":" + characterSlug;
        var result = storyRepository.findRelationshipsByCharacterId(characterId);
        return result == null
                ? Response.status(Status.NOT_FOUND).build()
                : Response.ok(result).build();
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAiCharacterRelationships(@PathParam("threadId") String threadId,
            @PathParam("characterSlug") String characterSlug) {
        String characterId = threadId + ":" + characterSlug;
        return invokeToolFunction(() -> storyTools.getCharacterRelationships(characterId));
    }

    /**
     * Get AI tool output for story network.
     * Returns the formatted text that AI tools see.
     */
    @GET
    @Path("/story/{threadId}/ai/network")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAiStoryNetwork(@PathParam("threadId") String threadId) {
        return invokeToolFunction(() -> storyTools.getStoryNetwork(threadId));
    }

    /**
     * Get AI tool output for location connections.
     * Returns the formatted text that AI tools see.
     */
    @GET
    @Path("/story/{threadId}/ai/locations/{locationSlug}/connections")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAiLocationConnections(@PathParam("threadId") String threadId,
            @PathParam("locationSlug") String locationSlug) {
        String locationId = threadId + ":" + locationSlug;
        return invokeToolFunction(() -> storyTools.getLocationConnections(locationId));
    }

    /**
     * Get AI tool output for shared history.
     * Returns the formatted text that AI tools see.
     */
    @GET
    @Path("/story/{threadId}/ai/shared-history/{char1Slug}/{char2Slug}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAiSharedHistory(@PathParam("threadId") String threadId,
            @PathParam("char1Slug") String char1Slug,
            @PathParam("char2Slug") String char2Slug) {
        String char1Id = threadId + ":" + char1Slug;
        String char2Id = threadId + ":" + char2Slug;
        return invokeToolFunction(() -> storyTools.getSharedHistory(char1Id, char2Id));
    }

    /**
     * Adapt text-oriented tool back to JSON-oriented API
     *
     * @param fn Tool function that returns a string
     * @return JSON-wrapped structure with clear(er) status indicators
     */
    Response invokeToolFunction(Supplier<String> fn) {
        try {
            var result = fn.get();
            if (result == null) {
                // none of these methods return null, so if this happens..
                return Response.serverError().entity(Map.of("error", Map.of("type", "UNEXPECTED"))).build();
            }
            if (result.matches("No .*? found .*")) {
                return Response.status(Status.NOT_FOUND)
                        .entity(Map.of("error", Map.of("type", "NOT_FOUND", "message", result)))
                        .build();
            }
            if (result.startsWith("Error: ")) {
                return Response.serverError()
                        .entity(Map.of("error", Map.of("type", "DATA_ACCESS", "message", result)))
                        .build();
            }
            return Response.ok().entity(Map.of("result", result)).build();
        } catch (Exception e) {
            Log.error("Error inspecting story tools", e);
            return Response.serverError()
                    .entity(Map.of("error", Map.of("type", "UNEXPECTED")))
                    .build();
        }
    }
}
