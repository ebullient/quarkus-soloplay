package dev.ebullient.soloplay.play.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Draft.ActorCreation.class, name = "actor_creation"),
        @JsonSubTypes.Type(value = Draft.ActiveActor.class, name = "active_actor")
})
public sealed interface Draft {

    public record ActorCreation(
            String name, String actorClass, Integer level,
            ActorDetails details,
            Boolean confirmed) implements Draft {
    };

    public record ActorDetails(
            String summary,
            String description,
            List<String> tags,
            List<String> aliases) {
    }

    record ActiveActor(String actorId) implements Draft {
    }

}
