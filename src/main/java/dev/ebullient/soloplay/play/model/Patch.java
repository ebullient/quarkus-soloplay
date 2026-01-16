package dev.ebullient.soloplay.play.model;

import java.util.List;

import dev.ebullient.soloplay.play.model.Draft.ActorDetails;

public sealed interface Patch {
    String rationale();

    List<String> sources();

    public record ActorCreationPatch(
            String name, String actorClass, Integer level,
            ActorDetails details,
            String rationale,
            List<String> sources) implements Patch {
    };
}
