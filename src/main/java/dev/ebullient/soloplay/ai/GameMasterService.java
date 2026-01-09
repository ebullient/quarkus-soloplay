package dev.ebullient.soloplay.ai;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.data.StoryThread;
import io.smallrye.mutiny.Multi;

/**
 * High-level service for GM interactions.
 * Handles context loading and delegates to PlayAgent.
 * Supports both blocking (REST) and streaming (WebSocket) modes.
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
     * Process a player message for a story thread (blocking).
     * Used by REST endpoint POST /api/story/play.
     *
     * @param storyThreadId The story thread slug
     * @param message The player's message
     * @return GM response as HTML (markdown converted)
     */
    public String chat(String storyThreadId, String message) {
        StoryThread thread = storyRepository.findStoryThreadById(storyThreadId);
        if (thread == null) {
            return "<p class='error'>Error: Story thread not found: " + storyThreadId + "</p>";
        }

        String response = playAgent.chat(
                thread.getId(),
                thread.getName(),
                thread.getCurrentDay(),
                thread.getAdventureName(),
                thread.getFollowingMode() != null ? thread.getFollowingMode().toString() : null,
                thread.getCurrentSituation(),
                message);

        thread.setLastPlayedAt(Instant.now());
        storyRepository.saveStoryThread(thread);

        return prettify.markdownToHtml(response);
    }

    /**
     * Result of a streaming chat operation.
     */
    public record StreamingChatResult(
            StoryThread storyThread,
            Multi<String> tokenStream) {
    }

    /**
     * Start a streaming chat session for a story thread.
     * Used by WebSocket endpoint /ws/story/{storyThreadId}.
     *
     * @param storyThreadId The story thread slug
     * @param message The player's message
     * @return StreamingChatResult with thread info and token stream, or null if not found
     */
    public StreamingChatResult chatStream(String storyThreadId, String message) {
        StoryThread thread = storyRepository.findStoryThreadById(storyThreadId);
        if (thread == null) {
            return null;
        }

        Multi<String> tokenStream = playAgent.chatStream(
                thread.getId(),
                thread.getName(),
                thread.getCurrentDay(),
                thread.getAdventureName(),
                thread.getFollowingMode() != null ? thread.getFollowingMode().toString() : null,
                thread.getCurrentSituation(),
                message);

        return new StreamingChatResult(thread, tokenStream);
    }

    /**
     * Update lastPlayedAt timestamp after a message exchange.
     *
     * @param storyThreadId The story thread slug
     */
    public void updateLastPlayed(String storyThreadId) {
        StoryThread thread = storyRepository.findStoryThreadById(storyThreadId);
        if (thread != null) {
            thread.setLastPlayedAt(Instant.now());
            storyRepository.saveStoryThread(thread);
        }
    }

    /**
     * Convert markdown to HTML.
     */
    public String markdownToHtml(String markdown) {
        return prettify.markdownToHtml(markdown);
    }
}
