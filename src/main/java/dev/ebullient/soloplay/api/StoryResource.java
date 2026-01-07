package dev.ebullient.soloplay.api;

import java.time.Instant;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestPath;

import dev.ebullient.soloplay.LoreRepository;
import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.ai.MarkdownAugmenter;
import dev.ebullient.soloplay.ai.PlayAssistant;
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
    PlayAssistant playAssistant;

    @Inject
    MarkdownAugmenter prettify;

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
     */
    @POST
    @Path("/play")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public String play(PlayRequest request) {
        // Load story thread by slug (primary ID)
        StoryThread storyThread = storyRepository.findStoryThreadBySlug(request.storyThreadId);
        if (storyThread == null) {
            return "<p class='error'>Error: Story thread not found: " + request.storyThreadId + "</p>";
        }

        // Generate conversation ID for memory (maintains chat history per thread)
        String conversationId = storyThread.getSlug() + "-play";

        // Call AI with full story context
        String response = playAssistant.chat(
                storyThread.getName(),
                storyThread.getSlug(),
                storyThread.getCurrentDay(),
                storyThread.getAdventureName(),
                storyThread.getFollowingMode() != null ? storyThread.getFollowingMode().toString() : null,
                storyThread.getCurrentSituation(),
                conversationId,
                request.message);

        // Update last played timestamp
        storyThread.setLastPlayedAt(Instant.now());
        storyRepository.saveStoryThread(storyThread);

        return prettify.markdownToHtml(response);
    }

    /**
     * Request model for play endpoint.
     */
    public record PlayRequest(String storyThreadId, String message) {
    }

    // ========== Story Thread CRUD Endpoints ==========

    /**
     * Get a specific story thread by slug.
     */
    @GET
    @Path("/{slug}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStoryThread(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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

        // Create character with tags
        List<String> tags = request.tags != null ? request.tags : List.of();
        Character character = storyRepository.createCharacter(
                slug,
                request.name,
                request.summary,
                request.description,
                tags);

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
     * Update an existing character.
     */
    @PUT
    @Path("/{slug}/characters/{characterId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCharacter(@RestPath String slug, @RestPath String characterId,
            UpdateCharacterRequest request) {
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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
            List<String> tags) {
    }

    public record UpdateCharacterRequest(
            String name,
            String summary,
            String description,
            String characterClass,
            Integer level,
            List<String> tags) {
    }

    public record ErrorResponse(String error) {
    }

    public record MessageResponse(String message) {
    }
}
