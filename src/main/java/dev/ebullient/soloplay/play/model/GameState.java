package dev.ebullient.soloplay.play.model;

import static dev.ebullient.soloplay.StringUtils.normalize;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;

@NodeEntity("Game")
public class GameState extends BaseEntity {

    public enum GamePhase {
        CHARACTER_CREATION,
        SCENE_INITIALIZATION,
        ACTIVE_PLAY,
        UNKNOWN;

        public GamePhase next() {
            return switch (this) {
                case CHARACTER_CREATION -> SCENE_INITIALIZATION;
                case SCENE_INITIALIZATION -> ACTIVE_PLAY;
                default -> GamePhase.UNKNOWN;
            };
        }
    }

    @Id
    String gameId;
    String adventureName;
    GamePhase gamePhase;

    // Gameplay state
    Integer turnNumber; // Increment each turn
    String currentLocation; // "location:docks"
    Set<String> plotFlags = new HashSet<>(); // Story state

    Long lastPlayedAt;

    @Transient
    Map<String, Draft> drafts = new HashMap<>();

    /**
     * @return the gameId
     */
    public String getGameId() {
        return gameId;
    }

    /**
     * @param gameId the gameId to set
     */
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    /**
     * @return the gamePhase
     */
    public GamePhase getGamePhase() {
        return gamePhase == null
                ? GamePhase.UNKNOWN
                : gamePhase;
    }

    /**
     * @param gamePhase the gamePhase to set
     */
    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }

    public Long getLastPlayedAt() {
        return lastPlayedAt;
    }

    public Integer getTurnNumber() {
        return turnNumber;
    }

    public void incrementTurn() {
        if (turnNumber == null) {
            turnNumber = 1;
        } else {
            turnNumber++;
        }
        this.lastPlayedAt = Instant.now().toEpochMilli();
    }

    public String getAdventureName() {
        return adventureName;
    }

    public void setAdventureName(String adventureName) {
        this.adventureName = adventureName;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public <T extends Draft> T getDraft(String key, Class<T> clazz) {
        Draft draft = drafts.get(key);
        if (clazz.isInstance(draft)) {
            return clazz.cast(draft);
        }
        return null;
    }

    public <T extends Draft> T getDraftOrDefault(String key, Class<T> clazz, T fallback) {
        Draft draft = drafts.getOrDefault(key, fallback);
        if (clazz.isInstance(draft)) {
            return clazz.cast(draft);
        }
        return fallback;
    }

    public <T extends Draft> void putDraft(String key, T value) {
        this.drafts.put(key, value);
    }

    public <T extends Draft> void removeDraft(String key) {
        this.drafts.remove(key);
    }

    public void addPlotFlag(String flag) {
        var normalized = normalize(flag);
        if (!normalized.isBlank()) {
            plotFlags.add(normalized);
        }
    }

    public boolean hasPlotFlag(String flag) {
        return plotFlags.contains(normalize(flag));
    }

    public Set<String> getPlotFlags() {
        return plotFlags == null ? Set.of() : plotFlags;
    }
}
