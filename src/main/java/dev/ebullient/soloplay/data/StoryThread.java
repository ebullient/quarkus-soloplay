package dev.ebullient.soloplay.data;

import java.time.Instant;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Represents a story thread (save file/playthrough) in a specific setting.
 * Multiple story threads can exist for the same setting.
 *
 * Uses slug as primary ID for human-friendly URLs and simpler data model.
 * Display name can be changed, but slug is immutable once created.
 */
@NodeEntity("StoryThread")
public class StoryThread {
    @Id
    private String slug; // Primary ID: URL-friendly identifier (e.g., "summer-2024-adventure")

    private String name; // Display name: e.g., "Summer 2024 Adventure!" (can be edited)
    private String settingName; // Links to RAG embeddings

    private StoryStatus status;

    private Instant createdAt;
    private Instant lastPlayedAt;

    private Long currentDay; // In-world timeline (e.g., Day 1, Day 2)
    private String currentSituation; // Brief summary of current state

    // Adventure context (optional - for published adventures)
    private String adventureName; // e.g., "Lost Mine of Phandelver" or "Light of Xaryxis"
    private FollowingMode followingMode; // How strictly to follow the adventure

    public StoryThread() {
        this.createdAt = Instant.now();
        this.lastPlayedAt = Instant.now();
        this.status = StoryStatus.ACTIVE;
        this.currentDay = 1L;
        this.followingMode = FollowingMode.LOOSE;
    }

    /**
     * Create a new story thread.
     * Slug will be auto-generated from name and must be made unique by caller.
     */
    public StoryThread(String name, String settingName) {
        this();
        this.name = name;
        this.slug = slugify(name);
        this.settingName = settingName;
    }

    /**
     * Convert a name into a URL-friendly slug.
     */
    public static String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "untitled";
        }
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .trim()
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }

    // Getters and setters

    /**
     * Get the slug (primary ID).
     * This is immutable after creation.
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Set the slug (primary ID).
     * Should only be called during initialization or migration.
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * For backwards compatibility with code expecting getId().
     * Returns the slug.
     */
    public String getId() {
        return slug;
    }

    /**
     * Get the display name (can be edited by user).
     */
    public String getName() {
        return name;
    }

    /**
     * Set the display name.
     * Note: This does NOT change the slug (slug is immutable).
     */
    public void setName(String name) {
        this.name = name;
        // Slug remains unchanged
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

    public String getAdventureName() {
        return adventureName;
    }

    public void setAdventureName(String adventureName) {
        this.adventureName = adventureName;
    }

    public FollowingMode getFollowingMode() {
        return followingMode;
    }

    public void setFollowingMode(FollowingMode followingMode) {
        this.followingMode = followingMode;
    }

    public enum StoryStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        ABANDONED
    }

    public enum FollowingMode {
        LOOSE, // Use adventure as inspiration, but let player drive story
        STRICT, // Follow adventure beats and structure
        INSPIRATION // Reference adventure only when player explicitly asks
    }
}
