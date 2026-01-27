package dev.ebullient.soloplay.play;

import dev.ebullient.soloplay.play.model.PlayerActorCreationPatch;
import dev.langchain4j.model.output.structured.Description;

public record ActorCreationResponse(
        @Description("Text response to the player in markdown format") String message,
        @Description("Updated character attributes; null or empty means no updates") PlayerActorCreationPatch patch) {
}
