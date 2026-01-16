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

import dev.ebullient.soloplay.play.model.Draft.LocationDraft;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@NodeEntity
public class Location extends BaseEntity {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance locationDetail(Location location);
    }

    @Id
    private String id; // human readable slug
    private String gameId;

    private String name;
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

    /**
     * Create a location from a draft.
     */
    public Location(String gameId, LocationDraft draft) {
        this();
        this.gameId = gameId;

        this.name = draft.name();
        this.id = gameId + ":" + slugify(this.name);

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

    public void addEvent(Event event) {
        if (events.add(event)) {
            event.getLocations().add(this);
        }
    }

    public String render() {
        return Templates.locationDetail(this).render();
    }
}
