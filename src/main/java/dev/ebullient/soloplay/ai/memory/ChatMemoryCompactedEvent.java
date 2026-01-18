package dev.ebullient.soloplay.ai.memory;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;

/**
 * CDI event fired when chat memory is compacted (older messages dropped).
 *
 * This event allows listeners to create durable story artifacts from
 * the dropped messages before they're lost.
 *
 * @param gameId The game whose memory was compacted
 * @param droppedMessages Messages that were removed during compaction (oldest first)
 */
public record ChatMemoryCompactedEvent(
        String gameId,
        List<ChatMessage> droppedMessages) {
}
