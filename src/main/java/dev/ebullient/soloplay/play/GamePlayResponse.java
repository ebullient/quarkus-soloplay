package dev.ebullient.soloplay.play;

import java.util.List;

import dev.ebullient.soloplay.play.model.Patch;
import dev.ebullient.soloplay.play.model.PendingRoll;

public record GamePlayResponse(
        String narration,
        PendingRoll pendingRoll,
        List<Patch> patches,
        List<String> sources,
        String turnSummary,
        String currentLocation,
        List<String> actorsPresent,
        List<String> locationsPresent) {
}
