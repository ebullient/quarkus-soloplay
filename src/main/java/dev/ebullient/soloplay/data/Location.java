package dev.ebullient.soloplay.data;

import java.time.Instant;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.id.UuidStrategy;

/**
 * Represents a location in the campaign world.
 */
@NodeEntity("Location")
public class Location {
    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    private String id;
    private String campaignId;
    private Instant createdAt;
    private Instant updatedAt;

    private String name;
    private String summary; // Short, stable identifier (e.g., "Ruined manor", "Bustling market")
    private String description; // Full narrative that can evolve over time

    private LocationType type;
    private LocationStatus status;

    @Relationship(type = "PART_OF")
    private Location parentLocation; // e.g., city within a region

    public Location() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = LocationStatus.ACTIVE;
    }

    public Location(String campaignId, LocationType type, String name) {
        this();
        this.campaignId = campaignId;
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

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public LocationType getType() {
        return type;
    }

    public void setType(LocationType type) {
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

    public LocationStatus getStatus() {
        return status;
    }

    public void setStatus(LocationStatus status) {
        this.status = status;
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

    public enum LocationType {
        CITY,
        TOWN,
        VILLAGE,
        DUNGEON,
        WILDERNESS,
        BUILDING,
        REGION,
        PLANE,
        OTHER
    }

    public enum LocationStatus {
        ACTIVE,
        DESTROYED,
        ABANDONED,
        HIDDEN,
        UNKNOWN
    }
}
