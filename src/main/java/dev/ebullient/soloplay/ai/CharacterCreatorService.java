package dev.ebullient.soloplay.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.data.StoryThread;

/**
 * High-level service for character creation interactions.
 * Handles context loading and delegates to CharacterCreatorAssistant.
 */
@ApplicationScoped
public class CharacterCreatorService {

    @Inject
    StoryRepository storyRepository;

    @Inject
    CharacterCreatorAssistant characterCreator;

    @Inject
    MarkdownAugmenter prettify;

    /**
     * Process a player message during character creation.
     * Automatically loads story context, calls AI, returns formatted response.
     *
     * @param storyThreadId The story thread slug
     * @param message The player's message
     * @return Character creator response as HTML (markdown converted)
     */
    public String chat(String storyThreadId, String message) {
        // Load story thread
        StoryThread thread = storyRepository.findStoryThreadById(storyThreadId);
        if (thread == null) {
            return "<p class='error'>Error: Story thread not found: " + storyThreadId + "</p>";
        }

        // Call AI with story context
        String response = characterCreator.chat(
                thread.getId(),
                thread.getName(),
                thread.getAdventureName(),
                message);

        return prettify.markdownToHtml(response);
    }

    /**
     * Generate an initial greeting for the character creation assistant.
     * This provides context-aware opening based on the adventure.
     *
     * @param storyThreadId The story thread slug
     * @return Initial greeting as HTML
     */
    public String getInitialGreeting(String storyThreadId) {
        StoryThread thread = storyRepository.findStoryThreadById(storyThreadId);
        if (thread == null) {
            return "<p class='error'>Error: Story thread not found: " + storyThreadId + "</p>";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Welcome the player to character creation. ");

        if (thread.getAdventureName() != null && !thread.getAdventureName().isBlank()) {
            prompt.append("This is for the adventure '")
                    .append(thread.getAdventureName())
                    .append("'. ");
            prompt.append("Look up any character creation guidance from the adventure ");
            prompt.append("(recommended classes, starting levels, backgrounds, etc.). ");
        }

        prompt.append("Introduce yourself and ask them about their character concept. ");
        prompt.append("What kind of character do they want to play?");

        return chat(storyThreadId, prompt.toString());
    }
}
