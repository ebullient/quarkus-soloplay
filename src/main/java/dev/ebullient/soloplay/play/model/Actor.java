package dev.ebullient.soloplay.play.model;

import static dev.ebullient.soloplay.StringUtils.normalize;
import static dev.ebullient.soloplay.StringUtils.slugify;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import dev.ebullient.soloplay.play.model.Draft.ActorDraft;
import dev.ebullient.soloplay.play.model.Patch.ActorPatch;
import dev.ebullient.soloplay.play.model.Patch.PlayerActorCreationPatch;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@NodeEntity
public class Actor extends BaseEntity {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance actorDetail(Actor actor);
    }

    @Id
    private String id; // human readable slug
    private String gameId;

    protected String name;
    protected String nameNormalized; // normalized (trimmed + lowercase) for indexed lookups
    protected String summary; // Short, stable identifier (e.g., "Aged wizard", "Young warrior")
    protected String description; // Full narrative that can evolve over time

    protected Set<String> aliases; // Alternative names (e.g., "Krux" for "Commodore Krux")

    @Relationship(type = "PARTICIPATED_IN", direction = Relationship.Direction.OUTGOING)
    protected Set<Event> events;

    public Actor() {
        super();
        this.aliases = new HashSet<>();
        this.events = new HashSet<>();
    }

    /**
     * Create a character from a draft.
     */
    public Actor(String gameId, ActorDraft draft) {
        this();
        this.gameId = gameId;

        this.name = draft.name();
        this.nameNormalized = this.name == null ? null : normalize(this.name);
        this.id = gameId + ":" + slugify(this.name);

        if (draft.details() != null) {
            this.summary = draft.details().summary();
            this.description = draft.details().description();
            setAliases(draft.details().aliases());
            setTags(draft.details().tags());
        }
    }

    public Actor(String gameId, ActorPatch p) {
        this();
        this.gameId = gameId;

        this.name = p.name();
        this.nameNormalized = this.name == null ? null : normalize(this.name);
        this.id = gameId + ":" + slugify(this.name);

        if (p.details() != null) {
            this.summary = p.details().summary();
            this.description = p.details().description();
            setAliases(p.details().aliases());
            setTags(p.details().tags());
        }
    }

    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
        markDirty();
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
        this.nameNormalized = name == null ? null : normalize(name);
        markDirty();
        // Note: id is NOT updated when name changes (id is immutable)
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
        markDirty();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        markDirty();
    }

    public Collection<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = new HashSet<>(normalize(aliases));
        this.markDirty();
    }

    /**
     * Add an alias to this character (case-insensitive, normalized to lowercase).
     */
    public void addAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        String normalized = normalize(alias);
        if (aliases.add(normalized)) {
            this.markDirty();
        }
    }

    /**
     * Remove an alias from this character (case-insensitive).
     */
    public void removeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        String normalized = normalize(alias);
        if (aliases.remove(normalized)) {
            this.markDirty();
        }
    }

    /**
     * Check if this character has a specific alias (case-insensitive).
     */
    public boolean hasAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return false;
        }
        String normalized = normalize(alias);
        return aliases.contains(normalized);
    }

    /**
     * Check if a name matches this character's name or any alias (case-insensitive).
     */
    public boolean matchesNameOrAlias(String nameOrAlias) {
        if (nameOrAlias == null || nameOrAlias.isBlank()) {
            return false;
        }
        String normalized = normalize(nameOrAlias);
        return name.toLowerCase().equals(normalized) || aliases.contains(normalized);
    }

    public Set<Event> getEvents() {
        return events;
    }

    public void addEvent(Event event) {
        if (events.add(event)) {
            event.getParticipants().add(this);
        }
    }

    public String render() {
        return Templates.actorDetail(this).render();
    }

    public Actor merge(ActorPatch p) {
        if (!name.equals(p.name())) {
            addAlias(p.name());
        }
        if (p.details() != null) {
            var details = p.details();
            if (details.summary() != null) {
                this.summary = p.details().summary();
            }
            if (details.description() != null) {
                this.description = p.details().description();
            }
            if (details.aliases() == null || details.aliases().isEmpty()) {
                setAliases(details.aliases());
            }
            if (details.tags() == null || details.tags().isEmpty()) {
                setTags(details.tags());
            }
        }
        markDirty();
        return this;
    }

    public Actor merge(PlayerActorCreationPatch p) {
        if (!name.equals(p.name())) {
            addAlias(p.name());
        }
        if (p.details() != null) {
            var details = p.details();
            if (details.summary() != null) {
                this.summary = p.details().summary();
            }
            if (details.description() != null) {
                this.description = p.details().description();
            }
            if (details.aliases() == null || details.aliases().isEmpty()) {
                setAliases(details.aliases());
            }
            if (details.tags() == null || details.tags().isEmpty()) {
                setTags(details.tags());
            }
        }
        markDirty();
        return this;
    }
}
