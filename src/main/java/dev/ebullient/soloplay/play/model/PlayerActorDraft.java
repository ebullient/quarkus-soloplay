package dev.ebullient.soloplay.play.model;

import java.util.List;

public record PlayerActorDraft(
        String name,
        String actorClass,
        Integer level,
        String summary,
        String description,
        List<String> tags,
        List<String> aliases,
        Boolean confirmed) implements Stash {

    public PlayerActorDraft(PlayerActorDraft old, boolean confirmed) {
        this(old.name(), old.actorClass(), old.level(),
                old.summary(), old.description(), old.tags(), old.aliases(),
                confirmed);
    }

    public Patch toPatch() {
        return new Patch("actor",
                this.name(),
                this.summary(),
                this.description(),
                this.tags(),
                this.aliases(),
                null);
    }
};
