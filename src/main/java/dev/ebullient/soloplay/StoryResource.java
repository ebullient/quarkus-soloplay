package dev.ebullient.soloplay;

import java.time.Instant;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
        StoryThread thread = storyRepository.findStoryThreadBySlug(request.storyThreadId);
        if (thread == null) {
            return "<p class='error'>Error: Story thread not found: " + request.storyThreadId + "</p>";
        }

        // Generate conversation ID for memory (maintains chat history per thread)
        String conversationId = thread.getSlug() + "-play";

        // Call AI with full story context
        String response = playAssistant.chat(
                thread.getSettingName(),
                thread.getName(),
                thread.getSlug(),
                thread.getCurrentDay(),
                thread.getAdventureName(),
                thread.getAdventureDescription(),
                thread.getFollowingMode() != null ? thread.getFollowingMode().toString() : null,
                thread.getCurrentSituation(),
                conversationId,
                request.message);

        // Update last played timestamp
        thread.setLastPlayedAt(Instant.now());
        storyRepository.saveStoryThread(thread);

        return prettify.markdownToHtml(response);
    }

    /**
     * Request model for play endpoint.
     */
    public record PlayRequest(String storyThreadId, String message) {
    }
}
