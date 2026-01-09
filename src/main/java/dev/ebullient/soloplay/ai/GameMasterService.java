package dev.ebullient.soloplay.ai;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.data.StoryThread;

/**
 * High-level service for GM interactions.
 * Handles context loading and delegates to PlayAssistant.
 */
@ApplicationScoped
public class GameMasterService {

    @Inject
    StoryRepository storyRepository;

    @Inject
    PlayAgent playAgent;

    @Inject
    MarkdownAugmenter prettify;

    /**
     * Process a player message for a story thread.
     * Automatically loads story context, calls AI, updates timestamp.
     *
     * @param storyThreadId The story thread slug
     * @param message The player's message
     * @return GM response as HTML (markdown converted)
     */
    public String chat(String storyThreadId, String message) {
        // Load story thread
        StoryThread thread = storyRepository.findStoryThreadById(storyThreadId);
        if (thread == null) {
            return "<p class='error'>Error: Story thread not found: " + storyThreadId + "</p>";
        }

        // Call AI with full story context
        String response = playAgent.chat(
                thread.getId(),
                thread.getName(),
                thread.getCurrentDay(),
                thread.getAdventureName(),
                thread.getFollowingMode() != null ? thread.getFollowingMode().toString() : null,
                thread.getCurrentSituation(),
                message);

        // Update last played timestamp
        thread.setLastPlayedAt(Instant.now());
        storyRepository.saveStoryThread(thread);

        return prettify.markdownToHtml(response);
    }
}
