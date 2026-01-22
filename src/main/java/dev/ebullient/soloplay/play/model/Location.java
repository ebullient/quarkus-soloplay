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

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@NodeEntity
public class Location extends BaseEntity {

    @CheckedTemplate(basePath = "models")
    public static class Templates {
        public static native TemplateInstance locationDetail(Location location);
    }

    @Id
    private String id; // human readable slug
    private String gameId;

    private String name;
    private String nameNormalized; // normalized (trimmed + lowercase) for indexed lookups
    private String summary; // Short, stable identifier (e.g., "Ruined manor", "Bustling market")
    private String description; // Full narrative that can evolve over time

    private Set<String> aliases; // Alternative names (e.g., "Krux" for "Commodore Krux")

    @Relationship(type = "OCCURRED_AT", direction = Relationship.Direction.INCOMING)
    private Set<Event> events;

    public Location() {
        super();
        this.aliases = new HashSet<>();
        this.events = new HashSet<>();
    }

    public Location(String gameId, Patch p) {
        this();
        this.gameId = gameId;

        this.name = p.name();
        this.nameNormalized = this.name == null ? null : normalize(this.name);
        this.id = gameId + ":" + slugify(this.name);
        this.summary = p.summary();
        this.description = p.description();
        setAliases(p.aliases());
        setTags(p.tags());
        markDirty();
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
     * Check if a name matches this location's name or any alias (case-insensitive).
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

    /** Note: does not update event */
    public void addEvent(Event event) {
        if (events.add(event)) {
            markDirty();
        }
    }

    /** Note: does not update event */
    public void removeEvent(Event event) {
        if (events.remove(event)) {
            markDirty();
        }
    }

    public String render() {
        return Templates.locationDetail(this).render();
    }

    public Location merge(Patch p) {
        if (!name.equals(p.name())) {
            addAlias(p.name());
        }
        if (p.summary() != null) {
            this.summary = p.summary();
        }
        if (p.description() != null) {
            this.description = p.description();
        }
        if (p.aliases() == null || p.aliases().isEmpty()) {
            setAliases(p.aliases());
        }
        if (p.tags() == null || p.tags().isEmpty()) {
            setTags(p.tags());
        }
        markDirty();
        return this;
    }
}
