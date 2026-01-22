package dev.ebullient.soloplay.play.model;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

public record PendingRoll(
        String type, // "skill_check", "attack", "saving_throw", "ability_check"
        String skill, // "persuasion", "stealth", etc. (null for attacks/saves)
        String ability, // "strength", "dexterity", etc.
        Integer dc, // null if contested or attack roll
        String target, // who/what this is against
        String context) implements Stash { // brief explanation for player

    @CheckedTemplate(basePath = "models")
    public static class Templates {
        public static native TemplateInstance pendingRoll(PendingRoll roll);
    }

    public String render() {
        return Templates.pendingRoll(this).render();
    }
}
