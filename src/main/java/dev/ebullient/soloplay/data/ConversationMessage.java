package dev.ebullient.soloplay.data;

import java.time.Instant;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Persisted conversation message for UI history.
 *
 * This is separate from LangChain4j chat memory (which is a bounded window for prompting).
 * This transcript is append-only and provides durable user-facing chat history.
 *
 * ID format: "{storyThreadId}:{seq}" where seq is monotonically increasing per thread.
 */
@NodeEntity("ConversationMessage")
public class ConversationMessage {

    @Id
    private String id; // Composite: "{storyThreadId}:{seq}"

    private String storyThreadId;
    private Long seq; // Monotonic sequence number per story thread

    private String role; // "user" or "assistant"
    private String markdown; // Original markdown content
    private String html; // Rendered HTML content

    private Instant timestamp;

    public ConversationMessage() {
        this.timestamp = Instant.now();
    }

    public ConversationMessage(String storyThreadId, Long seq, String role, String markdown, String html) {
        this();
        this.storyThreadId = storyThreadId;
        this.seq = seq;
        this.id = storyThreadId + ":" + seq;
        this.role = role;
        this.markdown = markdown;
        this.html = html;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public String getStoryThreadId() {
        return storyThreadId;
    }

    public Long getSeq() {
        return seq;
    }

    public String getRole() {
        return role;
    }

    public String getMarkdown() {
        return markdown;
    }

    public String getHtml() {
        return html;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
