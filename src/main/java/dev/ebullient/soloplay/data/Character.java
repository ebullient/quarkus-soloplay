package dev.ebullient.soloplay.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.id.UuidStrategy;

/**
 * Represents a PC or NPC in the campaign.
 * <p>
 * Characters use a tag-based classification system instead of rigid enums.
 * Common tags include:
 * <ul>
 * <li>Control: "player-controlled", "npc"</li>
 * <li>Party: "companion", "temporary", "protagonist"</li>
 * <li>Status: "dead", "missing", "imprisoned", "retired"</li>
 * <li>Roles: "quest-giver", "merchant", "informant", "villain", "mentor"</li>
 * <li>Prefixed: "faction:thieves-guild", "profession:blacksmith", "location:tavern"</li>
 * </ul>
 */
@NodeEntity("Character")
public class Character {
    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    private String id;
    private Instant createdAt;
    private Instant updatedAt;

    private String storyThreadId;

    private List<String> tags;
    private String name;
    private String summary; // Short, stable identifier (e.g., "Aged wizard", "Young warrior")
    private String description; // Full narrative that can evolve over time
    private String characterClass; // e.g., "Fighter", "Wizard"
    private Integer level;
    private String alignment;

    public Character() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.tags = new ArrayList<>();
    }

    public Character(String storyThreadId, String name) {
        this();
        this.storyThreadId = storyThreadId;
        this.name = name;
        // Default to NPC if no tags specified
        this.tags.add("npc");
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    /**
     * Add a tag to this character (case-insensitive, normalized to lowercase).
     */
    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return;
        }
        String normalized = tag.trim().toLowerCase();
        if (!tags.contains(normalized)) {
            tags.add(normalized);
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Remove a tag from this character (case-insensitive).
     */
    public void removeTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return;
        }
        String normalized = tag.trim().toLowerCase();
        if (tags.remove(normalized)) {
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Check if this character has a specific tag (case-insensitive).
     */
    public boolean hasTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String normalized = tag.trim().toLowerCase();
        return tags.contains(normalized);
    }

    /**
     * Check if this character has any of the provided tags (case-insensitive).
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
     * Check if this character has all of the provided tags (case-insensitive).
     */
    public boolean hasAllTags(List<String> checkTags) {
        if (checkTags == null || checkTags.isEmpty()) {
            return true;
        }
        return checkTags.stream()
                .map(t -> t.trim().toLowerCase())
                .allMatch(tags::contains);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public String getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(String characterClass) {
        this.characterClass = characterClass;
        this.updatedAt = Instant.now();
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
        this.updatedAt = Instant.now();
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
