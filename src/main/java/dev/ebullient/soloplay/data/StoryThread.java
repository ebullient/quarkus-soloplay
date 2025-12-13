package dev.ebullient.soloplay.data;

import java.time.Instant;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.id.UuidStrategy;

/**
 * Represents a story thread (save file/playthrough) in a specific setting.
 * Multiple story threads can exist for the same setting.
 */
@NodeEntity("StoryThread")
public class StoryThread {
    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    private String id;

    private String name; // e.g., "Summer 2024 Adventure"
    private String settingName; // Links to RAG embeddings

    private StoryStatus status;

    private Instant createdAt;
    private Instant lastPlayedAt;

    private Long currentDay; // In-world timeline (e.g., Day 1, Day 2)
    private String currentSituation; // Brief summary of current state

    public StoryThread() {
        this.createdAt = Instant.now();
        this.lastPlayedAt = Instant.now();
        this.status = StoryStatus.ACTIVE;
        this.currentDay = 1L;
    }

    public StoryThread(String name, String settingName) {
        this();
        this.name = name;
        this.settingName = settingName;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSettingName() {
        return settingName;
    }

    public void setSettingName(String settingName) {
        this.settingName = settingName;
    }

    public StoryStatus getStatus() {
        return status;
    }

    public void setStatus(StoryStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastPlayedAt() {
        return lastPlayedAt;
    }

    public void setLastPlayedAt(Instant lastPlayedAt) {
        this.lastPlayedAt = lastPlayedAt;
    }

    public Long getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(Long currentDay) {
        this.currentDay = currentDay;
    }

    public String getCurrentSituation() {
        return currentSituation;
    }

    public void setCurrentSituation(String currentSituation) {
        this.currentSituation = currentSituation;
    }

    public enum StoryStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        ABANDONED
    }
}
