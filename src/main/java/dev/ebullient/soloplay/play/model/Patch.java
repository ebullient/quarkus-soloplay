package dev.ebullient.soloplay.play.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import dev.ebullient.soloplay.play.model.Draft.Details;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Patch.PlayerActorCreationPatch.class, name = "player_actor"),
        @JsonSubTypes.Type(value = Patch.ActorPatch.class, name = "actor"),
        @JsonSubTypes.Type(value = Patch.LocationPatch.class, name = "location"),
})
public sealed interface Patch {

    public record PlayerActorCreationPatch(
            String name, String actorClass, Integer level,
            Details details,
            String rationale,
            List<String> sources) implements Patch {
    };

    public record ActorPatch(
            String name,
            Details details) implements Patch {
    };

    public record LocationPatch(
            String name,
            Details details) implements Patch {
    };
}
