package dev.ebullient.soloplay.ai.memory;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.data.StoryThread;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * Listens for chat memory compaction events and creates StoryEvents
 * to preserve important context that would otherwise be lost.
 *
 * When the chat memory window overflows and older messages are dropped,
 * this listener creates a summary event tagged with "memory" and "summary".
 */
@ApplicationScoped
public class ChatMemoryCompactionListener {
    private static final Logger LOG = Logger.getLogger(ChatMemoryCompactionListener.class);

    private static final int MAX_EXCERPT_LENGTH = 2000;

    @Inject
    StoryRepository storyRepository;

    /**
     * Handle chat memory compaction by creating a StoryEvent.
     */
    public void onCompaction(@Observes ChatMemoryCompactedEvent event) {
        String storyThreadId = event.storyThreadId();
        List<ChatMessage> droppedMessages = event.droppedMessages();

        LOG.infof("Processing compaction for %s: %d messages to summarize",
                storyThreadId, droppedMessages.size());

        // Get the current day from the story thread
        StoryThread thread = storyRepository.findStoryThreadById(storyThreadId);
        Long currentDay = thread != null ? thread.getCurrentDay() : null;

        // Create a summary of the dropped messages
        String summary = createSummary(droppedMessages);

        // Create a StoryEvent to preserve this context
        storyRepository.createEvent(
                storyThreadId,
                "Memory Summary: Chat History Archived",
                summary,
                currentDay,
                null, // no specific participants
                null, // no specific locations
                List.of("memory", "summary", "auto-generated"));

        LOG.infof("Created memory summary event for %s", storyThreadId);
    }

    /**
     * Create a summary of the dropped messages.
     * MVP implementation: extract and truncate the conversation text.
     */
    private String createSummary(List<ChatMessage> messages) {
        StringBuilder summary = new StringBuilder();
        summary.append("**Archived conversation excerpt:**\n\n");

        for (ChatMessage message : messages) {
            String role = getRole(message);
            String text = getText(message);

            summary.append("**").append(role).append(":** ");
            summary.append(text);
            summary.append("\n\n");

            // Stop if we've accumulated enough content
            if (summary.length() > MAX_EXCERPT_LENGTH) {
                break;
            }
        }

        String result = summary.toString();
        if (result.length() > MAX_EXCERPT_LENGTH) {
            result = result.substring(0, MAX_EXCERPT_LENGTH) + "\n\n*[truncated]*";
        }

        return result;
    }

    private String getRole(ChatMessage message) {
        return switch (message) {
            case UserMessage um -> "Player";
            case AiMessage am -> "GM";
            default -> message.type().name();
        };
    }

    private String getText(ChatMessage message) {
        return switch (message) {
            case UserMessage um -> um.singleText();
            case AiMessage am -> am.text();
            default -> "[message content not extractable]";
        };
    }
}
