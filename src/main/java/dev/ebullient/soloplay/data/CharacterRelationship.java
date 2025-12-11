package dev.ebullient.soloplay.data;

import java.time.Instant;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * Represents a relationship between two characters with metadata.
 * This is a reified relationship - a relationship that has its own properties.
 */
@RelationshipEntity
public class CharacterRelationship {
    @Id
    @GeneratedValue
    private Long id; // OGM needs a Long id for relationships
    private Instant since;
    private Instant updatedAt;

    @StartNode
    private Character from;

    @EndNode
    private Character to;

    private RelationType type;
    private String description;
    private Integer strength; // -10 (enemy) to +10 (close ally)

    public CharacterRelationship() {
        this.since = Instant.now();
        this.updatedAt = Instant.now();
        this.strength = 0;
    }

    public CharacterRelationship(Character from, Character to, RelationType type) {
        this();
        this.from = from;
        this.to = to;
        this.type = type;
    }

    // Getters and setters
    public Character getFrom() {
        return from;
    }

    public void setFrom(Character from) {
        this.from = from;
    }

    public Character getTo() {
        return to;
    }

    public void setTo(Character to) {
        this.to = to;
    }

    public RelationType getType() {
        return type;
    }

    public void setType(RelationType type) {
        this.type = type;
        this.updatedAt = Instant.now();
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

    public enum RelationType {
        KNOWS,
        ALLY,
        ENEMY,
        FAMILY,
        ROMANTIC,
        MENTOR,
        STUDENT,
        EMPLOYER,
        EMPLOYEE,
        FRIEND,
        RIVAL,
        LOCATED_AT,
        OWNS,
        MEMBER_OF
    }
}
