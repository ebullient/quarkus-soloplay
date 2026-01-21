package dev.ebullient.soloplay.play;

import dev.ebullient.soloplay.play.model.PlayerActorCreationPatch;

public record ActorCreationResponse(String messageMarkdown, PlayerActorCreationPatch patch) {
}
