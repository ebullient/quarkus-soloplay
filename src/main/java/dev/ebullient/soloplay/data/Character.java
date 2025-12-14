package dev.ebullient.soloplay.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Represents a PC or NPC in the campaign.
 * <p>
 * Uses composite slug-based ID: "{storyThreadSlug}:{characterSlug}"
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
    private String id; // Composite: "{storyThreadSlug}:{characterSlug}"

    private String slug; // Character slug part (e.g., "thorin-oakenshield", "tavern-guard")
    private String storyThreadId; // Story thread slug (e.g., "summer-adventure")

    private Instant createdAt;
    private Instant updatedAt;

    private List<String> tags;
    private String name; // Display name (mutable, e.g., "Thorin Oakenshield")
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

    /**
     * Create a character with a specific slug.
     * This is the preferred constructor when the AI provides a meaningful slug.
     */
    public Character(String storyThreadId, String slug, String name) {
        this();
        this.storyThreadId = storyThreadId;
        this.slug = slug;
        this.name = name;
        this.id = storyThreadId + ":" + slug;
        // Default to NPC if no tags specified
        this.tags.add("npc");
    }

    /**
     * Create a character with auto-generated slug from name.
     * Use this when no specific slug is provided.
     */
    public Character(String storyThreadId, String name) {
        this(storyThreadId, StoryThread.slugify(name), name);
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
        // Note: slug is NOT updated when name changes (slug is immutable)
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
        this.updatedAt = Instant.now();
    }
}
