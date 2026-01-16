package dev.ebullient.soloplay.play.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Draft.PlayerActorDraft.class, name = "actor_draft"),
        @JsonSubTypes.Type(value = Draft.LocationDraft.class, name = "location_draft"),
        @JsonSubTypes.Type(value = Draft.PendingRollDraft.class, name = "pending_roll")
})
public sealed interface Draft {

    public record ActorDraft(
            String name,
            Details details) implements Draft {
    };

    public record PlayerActorDraft(
            String name,
            Details details,
            String actorClass, Integer level,
            Boolean confirmed) implements Draft {

        public ActorDraft toActorDraft() {
            return new ActorDraft(name, details);
        }
    };

    public record LocationDraft(
            String name,
            Details details) implements Draft {
    };

    public record PendingRollDraft(
            String type,
            String skill,
            String ability,
            Integer dc,
            String target,
            String context) implements Draft {
    }

    public record Details(
            String summary,
            String description,
            List<String> tags,
            List<String> aliases) {
    }
}
