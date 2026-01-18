package dev.ebullient.soloplay.play.model;

import org.neo4j.ogm.annotation.NodeEntity;

import dev.ebullient.soloplay.play.model.Draft.PlayerActorDraft;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@NodeEntity
public class PlayerActor extends Actor {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance actorDetail(PlayerActor actor);
    }

    private String actorClass; // e.g., "Fighter", "Wizard"
    private Integer level;

    public PlayerActor() {
        super();
    }

    public PlayerActor(String gameId, PlayerActorDraft draft) {
        super(gameId, draft.toActorDraft());

        this.level = draft.level();
        this.actorClass = draft.actorClass();
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
