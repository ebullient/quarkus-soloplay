package dev.ebullient.soloplay.data;

import java.time.Instant;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.id.UuidStrategy;

/**
 * Represents a PC or NPC in the campaign.
 */
@NodeEntity("Character")
public class Character {
    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    private String id;
    private Instant createdAt;
    private Instant updatedAt;

    private String storyThreadId;

    private CharacterType type;
    private String name;
    private String summary; // Short, stable identifier (e.g., "Aged wizard", "Young warrior")
    private String description; // Full narrative that can evolve over time
    private String characterClass; // e.g., "Fighter", "Wizard"
    private Integer level;
    private String alignment;
    private CharacterStatus status;

    public Character() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = CharacterStatus.ACTIVE;
    }

    public Character(String storyThreadId, CharacterType type, String name) {
        this();
        this.storyThreadId = storyThreadId;
        this.type = type;
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

    public CharacterType getType() {
        return type;
    }

    public void setType(CharacterType type) {
        this.type = type;
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

    public CharacterStatus getStatus() {
        return status;
    }

    public void setStatus(CharacterStatus status) {
        this.status = status;
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

    public enum CharacterType {
        PC, // Player Character
        NPC, // Non-Player Character
        SIDEKICK // NPC that travels with party
    }

    public enum CharacterStatus {
        ACTIVE,
        RETIRED,
        DEAD,
        MISSING,
        UNKNOWN
    }
}
