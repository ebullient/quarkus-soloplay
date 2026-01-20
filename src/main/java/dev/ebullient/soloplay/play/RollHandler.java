package dev.ebullient.soloplay.play;

import jakarta.enterprise.context.ApplicationScoped;

import dev.ebullient.soloplay.play.GamePlayAssistant.PendingRoll;
import dev.ebullient.soloplay.play.GamePlayAssistant.RollResult;
import dev.ebullient.soloplay.play.model.GameState;

@ApplicationScoped
public class RollHandler {
    static final String DRAFT_KEY = "pending_roll";

    public RollResult handleRollCommand(GameState game, String trimmed) {
        PendingRoll pending = getPendingRoll(game);
        return null;
    }

    PendingRoll getPendingRoll(GameState game) {
        return game.getStash(DRAFT_KEY, PendingRoll.class);
    }

    void clearPendingRoll(GameState game) {
        game.removeStash(DRAFT_KEY);
    }

    void setPendingRoll(GameState game, PendingRoll pendingRoll) {
        game.putStash(DRAFT_KEY, pendingRoll);
    }
}
