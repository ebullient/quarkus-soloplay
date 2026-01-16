package dev.ebullient.soloplay.play;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;

import dev.ebullient.soloplay.play.model.Draft;

@NodeEntity("Game")
public class GameState {

    public enum GamePhase {
        CHARACTER_CREATION,
        UNKNOWN;

        GamePhase next() {
            return switch (this) {
                case CHARACTER_CREATION -> GamePhase.UNKNOWN;
                default -> GamePhase.UNKNOWN;
            };
        }
    }

    @Id
    String gameId;
    GamePhase gamePhase;
    Long lastPlayedAt;

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
        return gamePhase;
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

    public String getAdventureName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAdventureName'");
    }
}
