package dev.ebullient.soloplay.play;

import java.time.Instant;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity("Game")
public class GameState {

    public enum GamePhase {
        CHARACTER_CREATION
    }

    @Id
    String gameId;
    GamePhase gamePhase;
    Long lastPlayedAt;

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
}
