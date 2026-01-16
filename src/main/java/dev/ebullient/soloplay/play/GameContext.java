package dev.ebullient.soloplay.play;

import jakarta.enterprise.context.RequestScoped;

/**
 * Request-scoped holder for the current game context.
 * Set this before invoking AI services so that tools can access the gameId.
 */
@RequestScoped
public class GameContext {

    private String gameId;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
}
