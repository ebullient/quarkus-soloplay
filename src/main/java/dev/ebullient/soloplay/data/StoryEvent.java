package dev.ebullient.soloplay.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 * Represents an event that occurred during a game session.
 * <p>
 * Uses composite slug-based ID: "{storyThreadSlug}:{eventSlug}"
 * <p>
 * Events use a tag-based classification system instead of rigid enums.
 * Common tags include:
 * <ul>
 * <li>Activity: "combat", "social", "exploration", "rest", "travel"</li>
 * <li>Quest: "quest-start", "quest-complete", "quest-failed", "clue-discovered"</li>
 * <li>Character: "character-death", "level-up", "character-introduced", "character-departed"</li>
 * <li>Location: "location-discovered", "location-entered", "location-left"</li>
 * <li>Items: "item-acquired", "item-lost", "item-used", "treasure-found"</li>
 * <li>Narrative: "plot-twist", "revelation", "decision-made", "consequence"</li>
 * <li>Prefixed: "faction:thieves-guild", "tone:dramatic", "importance:critical"</li>
 * </ul>
 */
@NodeEntity("Event")
public class StoryEvent {
    @Id
    private String id; // Composite: "{storyThreadSlug}:{eventSlug}"

    private String slug; // Event slug part (e.g., "battle-at-helms-deep", "first-encounter")
    private String storyThreadId; // Story thread slug (e.g., "summer-adventure")

    private Instant timestamp; // Real-world timestamp
    private String conversationId; // Narrative thread this event belongs to

    private List<String> tags;

    private Long day; // In-world day number (e.g., Day 1, Day 2)
    private String inWorldDate; // Optional formatted in-world date (e.g., "3rd of Hammer, 1492 DR")

    private String title; // Brief event title (e.g., "Battle at Helm's Deep", "First encounter with Gandalf")
    private String description; // Detailed description of what happened

    @Relationship(type = "PARTICIPATED_IN", direction = Relationship.Direction.INCOMING)
    private List<Character> participants;

    @Relationship(type = "OCCURRED_AT")
    private Location location;

    public StoryEvent() {
        this.timestamp = Instant.now();
        this.participants = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    /**
     * Create an event with auto-generated slug from title.
     */
    public StoryEvent(String storyThreadId, String title) {
        this();
        this.storyThreadId = storyThreadId;
        this.slug = StoryThread.slugify(title);
        this.title = title;
        this.id = storyThreadId + ":" + this.slug;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
        if (this.storyThreadId != null) {
            this.id = this.storyThreadId + ":" + slug;
        }
    }

    public String getStoryThreadId() {
        return storyThreadId;
    }

    public void setStoryThreadId(String storyThreadId) {
        this.storyThreadId = storyThreadId;
        if (this.slug != null) {
            this.id = storyThreadId + ":" + this.slug;
        }
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Long getDay() {
        return day;
    }

    public void setDay(Long day) {
        this.day = day;
    }

    public String getInWorldDate() {
        return inWorldDate;
    }

    public void setInWorldDate(String inWorldDate) {
        this.inWorldDate = inWorldDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        // Note: slug is NOT updated when title changes (slug is immutable)
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Character> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Character> participants) {
        this.participants = participants;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    /**
     * Add a tag to this event (case-insensitive, normalized to lowercase).
     */
    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return;
        }
        String normalized = tag.trim().toLowerCase();
        if (!tags.contains(normalized)) {
            tags.add(normalized);
        }
    }

    /**
     * Remove a tag from this event (case-insensitive).
     */
    public void removeTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return;
        }
        String normalized = tag.trim().toLowerCase();
        tags.remove(normalized);
    }

    /**
     * Check if this event has a specific tag (case-insensitive).
     */
    public boolean hasTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String normalized = tag.trim().toLowerCase();
        return tags.contains(normalized);
    }

    /**
     * Check if this event has any of the provided tags (case-insensitive).
     */
    public boolean hasAnyTag(List<String> checkTags) {
        if (checkTags == null || checkTags.isEmpty()) {
            return false;
        }
        return checkTags.stream()
                .map(t -> t.trim().toLowerCase())
                .anyMatch(tags::contains);
    }

    /**
     * Check if this event has all of the provided tags (case-insensitive).
     */
    public boolean hasAllTags(List<String> checkTags) {
        if (checkTags == null || checkTags.isEmpty()) {
            return true;
        }
        return checkTags.stream()
                .map(t -> t.trim().toLowerCase())
                .allMatch(tags::contains);
    }
}
