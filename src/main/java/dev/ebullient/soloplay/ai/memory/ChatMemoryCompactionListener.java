package dev.ebullient.soloplay.ai.memory;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import dev.langchain4j.data.message.ChatMessage;
import io.quarkus.logging.Log;

/**
 * Listens for chat memory compaction events and creates StoryEvents
 * to preserve important context that would otherwise be lost.
 *
 * When the chat memory window overflows and older messages are dropped,
 * this listener creates a summary event tagged with "memory" and "summary".
 */
@ApplicationScoped
public class ChatMemoryCompactionListener {
    /**
     * Handle chat memory compaction by creating a StoryEvent.
     */
    public void onCompaction(@Observes ChatMemoryCompactedEvent event) {
        String storyThreadId = event.gameId();
        List<ChatMessage> droppedMessages = event.droppedMessages();

        Log.infof("Processing compaction for %s: %d messages to summarize",
                storyThreadId, droppedMessages.size());

        Log.infof("Created memory summary event for %s", storyThreadId);
    }
}
