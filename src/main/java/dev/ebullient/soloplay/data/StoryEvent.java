package dev.ebullient.soloplay.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.id.UuidStrategy;

/**
 * Represents an event that occurred during a game session.
 */
@NodeEntity("Event")
public class StoryEvent {
    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    private String id;
    private Instant timestamp; // Real-world timestamp

    private String storyThreadId;
    private String conversationId; // Narrative thread this event belongs to

    private EventType type;

    private Long storyDay; // In-world day number (e.g., Day 1, Day 2)
    private String inWorldDate; // Optional formatted in-world date (e.g., "3rd of Hammer, 1492 DR")

    private String description;

    @Relationship(type = "PARTICIPATED_IN", direction = Relationship.Direction.INCOMING)
    private List<Character> participants;

    @Relationship(type = "OCCURRED_AT")
    private Location location;

    public StoryEvent() {
        this.timestamp = Instant.now();
        this.participants = new ArrayList<>();
    }

    public StoryEvent(String storyThreadId, String conversationId, String description) {
        this();
        this.storyThreadId = storyThreadId;
        this.conversationId = conversationId;
        this.description = description;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoryThreadId() {
        return storyThreadId;
    }

    public void setStoryThreadId(String storyThreadId) {
        this.storyThreadId = storyThreadId;
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

    public Long getStoryDay() {
        return storyDay;
    }

    public void setStoryDay(Long storyDay) {
        this.storyDay = storyDay;
    }

    public String getInWorldDate() {
        return inWorldDate;
    }

    public void setInWorldDate(String inWorldDate) {
        this.inWorldDate = inWorldDate;
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

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public enum EventType {
        COMBAT,
        SOCIAL,
        EXPLORATION,
        QUEST_START,
        QUEST_COMPLETE,
        CHARACTER_DEATH,
        LOCATION_DISCOVERED,
        ITEM_ACQUIRED,
        ITEM_REMOVED,
        LEVEL_UP,
        OTHER
    }
}
