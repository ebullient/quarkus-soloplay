package dev.ebullient.soloplay.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * Represents a relationship between two characters with metadata.
 * This is a reified relationship - a relationship that has its own properties.
 * <p>
 * Note: Neo4j OGM requires RelationshipEntity to use auto-generated Long ID.
 * Unlike nodes (Character, Location), relationships cannot use composite slug-based IDs.
 * <p>
 * Relationships use a tag-based classification system instead of rigid enums.
 * Common tags include:
 * <ul>
 * <li>Social: "knows", "ally", "enemy", "friend", "rival", "acquaintance"</li>
 * <li>Family: "family", "parent", "child", "sibling", "spouse", "ancestor"</li>
 * <li>Professional: "mentor", "student", "employer", "employee", "colleague", "business-partner"</li>
 * <li>Romantic: "romantic", "lover", "ex-lover", "courting", "married"</li>
 * <li>Organizational: "member-of", "leader-of", "subordinate", "peer"</li>
 * <li>Emotional: "trusts", "distrusts", "fears", "respects", "admires", "despises"</li>
 * <li>Prefixed: "faction:thieves-guild", "status:secret", "intensity:strong"</li>
 * </ul>
 */
@RelationshipEntity(type = "HAS_RELATIONSHIP")
public class CharacterRelationship {
    @Id
    @GeneratedValue
    private Long id; // OGM requires Long ID for relationship entities
    private Instant since;
    private Instant updatedAt;

    @StartNode
    private Character character1;

    @EndNode
    private Character character2;

    private List<String> tags;
    private String description;
    private Integer strength; // -10 (enemy) to +10 (close ally)

    public CharacterRelationship() {
        this.since = Instant.now();
        this.updatedAt = Instant.now();
        this.strength = 0;
        this.tags = new ArrayList<>();
    }

    public CharacterRelationship(Character character1, Character character2, List<String> tags) {
        this();
        this.character1 = character1;
        this.character2 = character2;
        if (tags != null && !tags.isEmpty()) {
            tags.forEach(this::addTag);
        }
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Character getCharacter1() {
        return character1;
    }

    public void setCharacter1(Character character1) {
        this.character1 = character1;
    }

    public Character getCharacter2() {
        return character2;
    }

    public void setCharacter2(Character character2) {
        this.character2 = character2;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    /**
     * Add a tag to this relationship (case-insensitive, normalized to lowercase).
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
     * Remove a tag from this relationship (case-insensitive).
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
     * Check if this relationship has a specific tag (case-insensitive).
     */
    public boolean hasTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String normalized = tag.trim().toLowerCase();
        return tags.contains(normalized);
    }

    /**
     * Check if this relationship has any of the provided tags (case-insensitive).
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
     * Check if this relationship has all of the provided tags (case-insensitive).
     */
    public boolean hasAllTags(List<String> checkTags) {
        if (checkTags == null || checkTags.isEmpty()) {
            return true;
        }
        return checkTags.stream()
                .map(t -> t.trim().toLowerCase())
                .allMatch(tags::contains);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public Integer getStrength() {
        return strength;
    }

    public void setStrength(Integer strength) {
        this.strength = strength;
        this.updatedAt = Instant.now();
    }

    public Instant getSince() {
        return since;
    }

    public void setSince(Instant since) {
        this.since = since;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
