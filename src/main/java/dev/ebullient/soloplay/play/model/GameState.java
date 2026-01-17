package dev.ebullient.soloplay.play.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;

@NodeEntity("Game")
public class GameState {

    public enum GamePhase {
        CHARACTER_CREATION,
        UNKNOWN;

        public GamePhase next() {
            return switch (this) {
                case CHARACTER_CREATION -> UNKNOWN;
                default -> GamePhase.UNKNOWN;
            };
        }
    }

    @Id
    String gameId;
    GamePhase gamePhase;
    Long lastPlayedAt;
    String adventureName;

    @Transient
    Map<String, Draft> drafts;

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

    public void setLastPlayedAt(Long lastPlayedAt) {
        this.lastPlayedAt = lastPlayedAt;
    }

    public void setLastPlayedAt(Instant now) {
        this.lastPlayedAt = now == null ? null : now.toEpochMilli();
    }

    public String getAdventureName() {
        return adventureName;
    }

    public void setAdventureName(String adventureName) {
        this.adventureName = adventureName;
    }

    /**
     * @return the drafts
     */
    public <T extends Draft> T getDraft(String key, Class<T> clazz) {
        if (drafts == null) {
            return null;
        }
        Draft draft = drafts.get(key);
        if (clazz.isInstance(draft)) {
            return clazz.cast(draft);
        }
        return null;
    }

    /**
     * @param drafts the drafts to set
     */
    public <T extends Draft> void putDraft(String key, T value) {
        if (drafts == null) {
            drafts = new HashMap<>();
        }
        this.drafts.put(key, value);
    }
}
