package dev.ebullient.soloplay.play.model;

import org.neo4j.ogm.annotation.NodeEntity;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@NodeEntity
public class PlayerActor extends Actor {

    @CheckedTemplate(basePath = "models")
    public static class Templates {
        public static native TemplateInstance playerActorDetail(PlayerActor actor);

        public static native TemplateInstance playerActorDraft(PlayerActorDraft draft);
    }

    private String actorClass; // e.g., "Fighter", "Wizard"
    private Integer level;

    public PlayerActor() {
        super();
    }

    public PlayerActor(String gameId, PlayerActorDraft draft) {
        super(gameId, draft.toPatch());

        this.level = draft.level();
        this.actorClass = draft.actorClass();
        markDirty();
    }

    public String getActorClass() {
        return actorClass;
    }

    public void setActorClass(String characterClass) {
        this.actorClass = characterClass;
        markDirty();
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
        markDirty();
    }
}
