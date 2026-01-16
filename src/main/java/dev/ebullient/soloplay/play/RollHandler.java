package dev.ebullient.soloplay.play;

import jakarta.enterprise.context.ApplicationScoped;

import dev.ebullient.soloplay.play.GamePlayAssistant.RollResult;
import dev.ebullient.soloplay.play.model.Draft.PendingRollDraft;
import dev.ebullient.soloplay.play.model.GameState;

@ApplicationScoped
public class RollHandler {
    static final String DRAFT_KEY = "pending_roll";

    public RollResult handleRollCommand(GameState game, String trimmed) {
        PendingRollDraft pending = getPendingRoll(game);
        return null;
    }

    PendingRollDraft getPendingRoll(GameState game) {
        return game.getDraft(DRAFT_KEY, PendingRollDraft.class);
    }

    void clearPendingRoll(GameState game) {
        game.removeDraft(DRAFT_KEY);
    }

    void setPendingRoll(GameState game, PendingRollDraft pendingRoll) {
        game.putDraft(DRAFT_KEY, pendingRoll);
    }
}
