package dev.ebullient.soloplay.play.model;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;

import dev.ebullient.soloplay.StringUtils;

@NodeEntity
public abstract class BaseEntity {

    protected Instant createdAt;
    protected Instant updatedAt;
    protected Set<String> tags;
    protected boolean dirty;

    protected BaseEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.tags = new HashSet<>();

        this.dirty = true; // must be persisted
    }

    public Collection<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = new HashSet<>(StringUtils.normalize(tags));
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    /**
     * Add a tag to this character (case-insensitive, normalized to lowercase).
     */
    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return;
        }
        String normalized = StringUtils.normalize(tag);
        if (tags.add(normalized)) {
            this.updatedAt = Instant.now();
            this.dirty = true;
        }
    }

    /**
     * Remove a tag from this character (case-insensitive).
     */
    public void removeTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return;
        }
        String normalized = StringUtils.normalize(tag);
        if (tags.remove(normalized)) {
            this.updatedAt = Instant.now();
            this.dirty = true;
        }
    }

    /**
     * Check if this character has a specific tag (case-insensitive).
     */
    public boolean hasTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String normalized = StringUtils.normalize(tag);
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

}
