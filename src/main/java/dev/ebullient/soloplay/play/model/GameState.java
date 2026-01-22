package dev.ebullient.soloplay.play.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
    Long lastPlayedAt;

    @Transient
    Map<String, Stash> stash = new HashMap<>();

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

    public <T extends Stash> T getStash(String key, Class<T> clazz) {
        Stash draft = stash.get(key);
        if (clazz.isInstance(draft)) {
            return clazz.cast(draft);
        }
        return null;
    }

    public <T extends Stash> T getStashOrDefault(String key, Class<T> clazz, T fallback) {
        Stash draft = stash.getOrDefault(key, fallback);
        if (clazz.isInstance(draft)) {
            return clazz.cast(draft);
        }
        return fallback;
    }

    public <T extends Stash> void putStash(String key, T value) {
        this.stash.put(key, value);
    }

    public <T extends Stash> void removeStash(String key) {
        this.stash.remove(key);
    }

    public Object dumpStash() {
        return this.stash;
    }
}
