package dev.ebullient.soloplay.api;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestPath;

import dev.ebullient.soloplay.LoreRepository;
import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.ai.CharacterCreatorService;
import dev.ebullient.soloplay.ai.GameMasterService;
import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.StoryThread;

/**
 * Story-specific data and operations for solo play.
 * Manages story threads and associated game elements (characters, locations, events, etc.).
 */
@ApplicationScoped
@Path("/api/story")
public class StoryResource {

    @Inject
    StoryRepository storyRepository;

    @Inject
    GameMasterService gameMaster;

    @Inject
    CharacterCreatorService characterCreator;

    @Inject
    LoreRepository loreRepository;

    /**
     * Get list of all story thread IDs.
     */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getStoryThreadIds() {
        return storyRepository.getStoryThreadIds();
    }

    /**
     * Story-aware chat endpoint for solo play.
     * Integrates RAG (lore), story tools (campaign state), and story thread context.
     *
     * Context loading and timestamp updates are handled by GameMasterService.
     */
    @POST
    @Path("/play")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public String play(PlayRequest request) {
        return gameMaster.chat(request.storyThreadId, request.message);
    }

    /**
     * Request model for play endpoint.
     */
    public record PlayRequest(String storyThreadId, String message) {
    }

    /**
     * Character creation chat endpoint.
     * Guides players through creating characters via conversational AI.
     *
     * @param request Contains storyThreadId and player message
     * @return Character creator response as HTML
     */
    @POST
    @Path("/character-creator")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public String characterCreator(PlayRequest request) {
        return characterCreator.chat(request.storyThreadId, request.message);
    }

    // ========== Story Thread CRUD Endpoints ==========

    /**
     * Get a specific story thread by slug.
     */
    @GET
    @Path("/{slug}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStoryThread(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }
        return Response.ok(thread).build();
    }

    /**
     * Create a new story thread.
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createStoryThread(CreateStoryThreadRequest request) {
        // Validate required fields
        if (request.name == null || request.name.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Story thread name is required"))
                    .build();
        }

        // Validate adventure exists if specified
        if (request.adventureName != null && !request.adventureName.isBlank()) {
            if (!loreRepository.validateAdventureExists(request.adventureName)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Adventure '" + request.adventureName
                                + "' not found. Use GET /api/lore/adventures to see available adventures."))
                        .build();
            }
        }

        // Create story thread with validation
        StoryThread thread;
        try {
            thread = storyRepository.createStoryThread(
                    request.name,
                    request.adventureName,
                    request.followingMode);
        } catch (IllegalArgumentException e) {
            // Handle slug conflict or invalid followingMode
            if (e.getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ErrorResponse(e.getMessage()))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse(e.getMessage()))
                        .build();
            }
        }

        return Response.status(Response.Status.CREATED).entity(thread).build();
    }

    /**
     * Update story thread runtime state.
     * Story threads are immutable once created - only runtime state can be updated.
     * Use this to track story progression (currentDay, currentSituation) or change status.
     */
    @PATCH
    @Path("/{slug}/state")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateStoryThreadState(@RestPath String slug, UpdateStoryThreadStateRequest request) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        // Update only runtime state fields
        if (request.currentSituation != null) {
            thread.setCurrentSituation(request.currentSituation.isBlank() ? null : request.currentSituation);
        }
        if (request.currentDay != null) {
            thread.setCurrentDay(request.currentDay);
        }
        if (request.status != null && !request.status.isBlank()) {
            try {
                thread.setStatus(StoryThread.StoryStatus.valueOf(request.status));
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid status: " + request.status
                                + ". Valid values: ACTIVE, PAUSED, COMPLETED, ABANDONED"))
                        .build();
            }
        }

        storyRepository.saveStoryThread(thread);
        return Response.ok(thread).build();
    }

    /**
     * Delete a story thread and all associated data.
     */
    @DELETE
    @Path("/{slug}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteStoryThread(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        storyRepository.deleteStoryThread(slug);
        return Response.ok(new MessageResponse("Story thread deleted: " + slug)).build();
    }

    // ========== Character CRUD Endpoints ==========

    /**
     * Get all characters for a story thread.
     */
    @GET
    @Path("/{slug}/characters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCharacters(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        List<Character> characters = storyRepository.findAllCharacters(slug);
        return Response.ok(characters).build();
    }

    /**
     * Get a specific character by ID.
     */
    @GET
    @Path("/{slug}/characters/{characterId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCharacter(@RestPath String slug, @RestPath String characterId) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        Character character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Character not found: " + characterId))
                    .build();
        }

        return Response.ok(character).build();
    }

    /**
     * Create a new character in a story thread.
     */
    @POST
    @Path("/{slug}/characters")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCharacter(@RestPath String slug, CreateCharacterRequest request) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        if (request.name == null || request.name.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Character name is required"))
                    .build();
        }

        if (request.summary == null || request.summary.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Character summary is required"))
                    .build();
        }

        // Create character with tags and aliases
        List<String> tags = request.tags != null ? request.tags : List.of();
        List<String> aliases = request.aliases != null ? request.aliases : List.of();
        Character character = storyRepository.createCharacter(
                slug,
                request.name,
                request.summary,
                request.description,
                tags,
                aliases);

        // Update optional fields if provided
        if ((request.characterClass != null && !request.characterClass.isBlank()) || request.level != null) {
            character = storyRepository.updateCharacter(
                    character.getId(),
                    null, // name - don't change
                    null, // summary - don't change
                    null, // description - don't change
                    request.characterClass,
                    request.level);
        }

        return Response.status(Response.Status.CREATED).entity(character).build();
    }

    /**
     * Update an existing character (partial update).
     * Only fields provided in the request will be updated.
     * Null or omitted fields will not be changed.
     * To update tags, include the complete tag list (replaces all existing tags).
     */
    @PATCH
    @Path("/{slug}/characters/{characterId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCharacter(@RestPath String slug, @RestPath String characterId,
            UpdateCharacterRequest request) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        Character character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Character not found: " + characterId))
                    .build();
        }

        // Update character fields
        character = storyRepository.updateCharacter(
                characterId,
                request.name,
                request.summary,
                request.description,
                request.characterClass,
                request.level);

        // Update tags if provided - replace all existing tags
        if (request.tags != null) {
            // Remove all existing tags
            List<String> currentTags = character.getTags();
            if (!currentTags.isEmpty()) {
                storyRepository.removeCharacterTags(characterId, currentTags);
            }

            // Add new tags
            if (!request.tags.isEmpty()) {
                storyRepository.addCharacterTags(characterId, request.tags);
            }

            // Reload character to get updated tags
            character = storyRepository.findCharacterById(characterId);
        }

        return Response.ok(character).build();
    }

    /**
     * Delete a character from a story thread.
     */
    @DELETE
    @Path("/{slug}/characters/{characterId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCharacter(@RestPath String slug, @RestPath String characterId) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        Character character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Character not found: " + characterId))
                    .build();
        }

        storyRepository.deleteCharacter(characterId);
        return Response.ok(new MessageResponse("Character deleted: " + characterId)).build();
    }

    // ========== Location CRUD Endpoints ==========

    /**
     * Get all locations for a story thread.
     */
    @GET
    @Path("/{slug}/locations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLocations(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        List<dev.ebullient.soloplay.data.Location> locations = storyRepository.findLocationsByStoryThreadId(slug);
        return Response.ok(locations).build();
    }

    /**
     * Get a specific location by ID.
     */
    @GET
    @Path("/{slug}/locations/{locationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLocation(@RestPath String slug, @RestPath String locationId) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        dev.ebullient.soloplay.data.Location location = storyRepository.findLocationById(locationId);
        if (location == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Location not found: " + locationId))
                    .build();
        }

        return Response.ok(location).build();
    }

    /**
     * Create a new location in a story thread.
     */
    @POST
    @Path("/{slug}/locations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLocation(@RestPath String slug, CreateLocationRequest request) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        if (request.name == null || request.name.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Location name is required"))
                    .build();
        }

        if (request.summary == null || request.summary.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Location summary is required"))
                    .build();
        }

        // Create location with tags
        List<String> tags = request.tags != null ? request.tags : List.of();
        dev.ebullient.soloplay.data.Location location = storyRepository.createLocation(
                slug,
                request.name,
                request.summary,
                request.description,
                tags);

        return Response.status(Response.Status.CREATED).entity(location).build();
    }

    /**
     * Update an existing location (partial update).
     * Only fields provided in the request will be updated.
     * Null or omitted fields will not be changed.
     * To update tags, include the complete tag list (replaces all existing tags).
     */
    @PATCH
    @Path("/{slug}/locations/{locationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLocation(@RestPath String slug, @RestPath String locationId,
            UpdateLocationRequest request) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        dev.ebullient.soloplay.data.Location location = storyRepository.findLocationById(locationId);
        if (location == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Location not found: " + locationId))
                    .build();
        }

        // Update location fields
        location = storyRepository.updateLocation(
                locationId,
                request.name,
                request.summary,
                request.description);

        // Update tags if provided - replace all existing tags
        if (request.tags != null) {
            // Remove all existing tags
            List<String> currentTags = location.getTags();
            if (!currentTags.isEmpty()) {
                storyRepository.removeLocationTags(locationId, currentTags);
            }

            // Add new tags
            if (!request.tags.isEmpty()) {
                storyRepository.addLocationTags(locationId, request.tags);
            }

            // Reload location to get updated tags
            location = storyRepository.findLocationById(locationId);
        }

        return Response.ok(location).build();
    }

    /**
     * Delete a location from a story thread.
     */
    @DELETE
    @Path("/{slug}/locations/{locationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLocation(@RestPath String slug, @RestPath String locationId) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Story thread not found: " + slug))
                    .build();
        }

        dev.ebullient.soloplay.data.Location location = storyRepository.findLocationById(locationId);
        if (location == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Location not found: " + locationId))
                    .build();
        }

        storyRepository.deleteLocation(locationId);
        return Response.ok(new MessageResponse("Location deleted: " + locationId)).build();
    }

    // ========== Request/Response Models ==========

    public record CreateStoryThreadRequest(
            String name,
            String adventureName,
            String followingMode) {
    }

    public record UpdateStoryThreadStateRequest(
            String currentSituation,
            Long currentDay,
            String status) {
    }

    public record CreateCharacterRequest(
            String name,
            String summary,
            String description,
            String characterClass,
            Integer level,
            List<String> tags,
            List<String> aliases) {
    }

    public record UpdateCharacterRequest(
            String name,
            String summary,
            String description,
            String characterClass,
            Integer level,
            List<String> tags) {
    }

    public record CreateLocationRequest(
            String name,
            String summary,
            String description,
            List<String> tags) {
    }

    public record UpdateLocationRequest(
            String name,
            String summary,
            String description,
            List<String> tags) {
    }

    public record ErrorResponse(String error) {
    }

    public record MessageResponse(String message) {
    }
}
