package dev.ebullient.soloplay.ai;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.data.Character;
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
     * Result of a character creation chat interaction.
     * Contains the HTML response and any newly created character.
     */
    public record ChatResult(String html, Character createdCharacter) {
    }

    /**
     * Process a player message during character creation.
     * Automatically loads story context, calls AI, and detects character creation.
     *
     * @param storyThreadId The story thread slug
     * @param message The player's message
     * @return ChatResult with HTML response and created character (if any)
     */
    public ChatResult chat(String storyThreadId, String message) {
        // Load story thread
        StoryThread thread = storyRepository.findStoryThreadById(storyThreadId);
        if (thread == null) {
            return new ChatResult(
                    "<p class='error'>Error: Story thread not found: " + storyThreadId + "</p>",
                    null);
        }

        // Get existing character IDs before the AI call
        Set<String> existingCharacterIds = storyRepository.findAllCharacters(storyThreadId)
                .stream()
                .map(Character::getId)
                .collect(Collectors.toSet());

        // Call AI with story context
        String response = characterCreator.chat(
                thread.getId(),
                thread.getName(),
                thread.getAdventureName(),
                message);

        String html = prettify.markdownToHtml(response);

        // Check if a new character was created by comparing before/after
        Character createdCharacter = null;
        List<Character> currentCharacters = storyRepository.findAllCharacters(storyThreadId);
        for (Character c : currentCharacters) {
            if (!existingCharacterIds.contains(c.getId())) {
                createdCharacter = c;
                // Ensure player-controlled tag is set (in case LLM forgot)
                if (!c.hasTag("player-controlled")) {
                    storyRepository.addCharacterTags(c.getId(), List.of("player-controlled"));
                }
                break;
            }
        }

        return new ChatResult(html, createdCharacter);
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

        // Initial greeting doesn't need character detection - just return the HTML
        return chat(storyThreadId, prompt.toString()).html();
    }
}
