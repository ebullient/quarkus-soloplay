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
 * Represents a location in the campaign world.
 * <p>
 * Locations use a tag-based classification system instead of rigid enums.
 * Common tags include:
 * <ul>
 * <li>Type: "city", "town", "village", "dungeon", "wilderness", "building", "region", "plane"</li>
 * <li>Status: "destroyed", "abandoned", "hidden", "active"</li>
 * <li>Features: "fortified", "magical", "haunted", "sacred", "cursed"</li>
 * <li>Access: "public", "restricted", "secret", "guarded"</li>
 * <li>Prefixed: "faction:thieves-guild", "climate:tropical", "terrain:mountainous"</li>
 * </ul>
 */
@NodeEntity("Location")
public class Location {
    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    private String id;
    private String storyThreadId;
    private Instant createdAt;
    private Instant updatedAt;

    private String name;
    private String summary; // Short, stable identifier (e.g., "Ruined manor", "Bustling market")
    private String description; // Full narrative that can evolve over time

    private List<String> tags;

    @Relationship(type = "PART_OF")
    private Location parentLocation; // e.g., city within a region

    public Location() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.tags = new ArrayList<>();
    }

    public Location(String storyThreadId, String name) {
        this();
        this.storyThreadId = storyThreadId;
        this.name = name;
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
     * Add a tag to this location (case-insensitive, normalized to lowercase).
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
     * Remove a tag from this location (case-insensitive).
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
     * Check if this location has a specific tag (case-insensitive).
     */
    public boolean hasTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String normalized = tag.trim().toLowerCase();
        return tags.contains(normalized);
    }

    /**
     * Check if this location has any of the provided tags (case-insensitive).
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
     * Check if this location has all of the provided tags (case-insensitive).
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

    public Location getParentLocation() {
        return parentLocation;
    }

    public void setParentLocation(Location parentLocation) {
        this.parentLocation = parentLocation;
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
