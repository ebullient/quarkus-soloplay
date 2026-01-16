package dev.ebullient.soloplay.play.model;

import static dev.ebullient.soloplay.StringUtils.slugify;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.Id;

import dev.ebullient.soloplay.StringUtils;
import dev.ebullient.soloplay.play.model.Draft.ActorCreation;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

public class Actor extends BaseEntity {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance actorDetail(Actor actor);
    }

    @Id
    private String id; // human readable slug
    private String gameId;

    private String name;
    private String summary; // Short, stable identifier (e.g., "Aged wizard", "Young warrior")
    private String description; // Full narrative that can evolve over time
    private String actorClass; // e.g., "Fighter", "Wizard"
    private Integer level;

    private Set<String> aliases; // Alternative names (e.g., "Krux" for "Commodore Krux")

    public Actor() {
        super();
        this.aliases = new HashSet<>();
    }

    /**
     * Create a character from a draft.
     */
    public Actor(String gameId, ActorCreation draft) {
        this();
        this.gameId = gameId;

        this.name = draft.name();
        this.id = gameId + ":" + slugify(this.name);

        this.level = draft.level();
        this.actorClass = draft.actorClass();

        if (draft.details() != null) {
            this.summary = draft.details().summary();
            this.description = draft.details().description();
            setAliases(draft.details().aliases());
            setTags(draft.details().tags());
        }
    }

    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    public String getGameId() {
        return gameId;
    }

    void setGameId(String gameId) {
        this.gameId = gameId;
        if (this.id != null) {
            this.id = gameId + ":" + slugify(name);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
        // Note: id is NOT updated when name changes (id is immutable)
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    public String getActorClass() {
        return actorClass;
    }

    public void setActorClass(String characterClass) {
        this.actorClass = characterClass;
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    protected void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = Instant.now();
    }

    public Collection<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = new HashSet<>(StringUtils.normalize(aliases));
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    /**
     * Add an alias to this character (case-insensitive, normalized to lowercase).
     */
    public void addAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        String normalized = StringUtils.normalize(alias);
        if (aliases.add(normalized)) {
            this.updatedAt = Instant.now();
            this.dirty = true;
        }
    }

    /**
     * Remove an alias from this character (case-insensitive).
     */
    public void removeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        String normalized = StringUtils.normalize(alias);
        if (aliases.remove(normalized)) {
            this.updatedAt = Instant.now();
            this.dirty = true;
        }
    }

    /**
     * Check if this character has a specific alias (case-insensitive).
     */
    public boolean hasAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return false;
        }
        String normalized = StringUtils.normalize(alias);
        return aliases.contains(normalized);
    }

    /**
     * Check if a name matches this character's name or any alias (case-insensitive).
     */
    public boolean matchesNameOrAlias(String nameOrAlias) {
        if (nameOrAlias == null || nameOrAlias.isBlank()) {
            return false;
        }
        String normalized = StringUtils.normalize(nameOrAlias);
        return name.toLowerCase().equals(normalized) || aliases.contains(normalized);
    }

    public String render() {
        return Templates.actorDetail(this).render();
    }
}
