package dev.ebullient.soloplay.play.model;

import java.util.List;

public record PlayerActorCreationPatch(
        String name, String actorClass, Integer level,
        String summary,
        String description,
        List<String> tags,
        List<String> aliases,
        String rationale,
        List<String> sources) implements Stash {
}
