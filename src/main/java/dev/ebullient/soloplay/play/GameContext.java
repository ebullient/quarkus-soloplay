package dev.ebullient.soloplay.play;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.play.model.GameState;

/**
 * Request-scoped holder for the current game context.
 * Set this before invoking AI services so that tools can access the gameId.
 */
@RequestScoped
public class GameContext {

    @Inject
    GameRepository gameRepository;

    private GameState gameState;
    private List<String> theParty;

    public void setGameState(GameState gameState, List<String> theParty) {
        this.gameState = gameState;
        this.theParty = theParty;
    }

    public String getGameId() {
        return gameState.getGameId();
    }

    public String getAdventureName() {
        return gameState.getAdventureName();
    }

    public String getCurrentLocation() {
        return gameState.getCurrentLocation();
    }

    public Collection<String> listPlayerCharacters() {
        return theParty;
    }
}
