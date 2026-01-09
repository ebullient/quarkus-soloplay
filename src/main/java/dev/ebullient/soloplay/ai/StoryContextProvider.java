package dev.ebullient.soloplay.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.data.StoryThread;

/**
 * Provides story thread context for AI assistant system messages.
 * Retrieves full StoryThread entity to populate template variables.
 */
@ApplicationScoped
public class StoryContextProvider {

    @Inject
    StoryRepository storyRepository;

    /**
     * Story context record with all fields needed for system message template.
     */
    public record StoryContext(
            String storyThreadId,
            String storyName,
            Long currentDay,
            String adventureName,
            String followingMode,
            String currentSituation) {
    }

    /**
     * Load story context for the given thread ID.
     *
     * @param storyThreadId The story thread slug
     * @return StoryContext with all fields, or null if thread not found
     */
    public StoryContext getContext(String storyThreadId) {
        StoryThread thread = storyRepository.findStoryThreadById(storyThreadId);
        if (thread == null) {
            return null;
        }

        return new StoryContext(
                thread.getId(),
                thread.getName(),
                thread.getCurrentDay(),
                thread.getAdventureName(),
                thread.getFollowingMode() != null ? thread.getFollowingMode().toString() : null,
                thread.getCurrentSituation());
    }
}
