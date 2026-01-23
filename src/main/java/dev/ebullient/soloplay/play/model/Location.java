package dev.ebullient.soloplay.play.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@NodeEntity
public class Location extends NamedBaseEntity {

    @CheckedTemplate(basePath = "models")
    public static class Templates {
        public static native TemplateInstance locationDetail(Location location);
    }

    @Relationship(type = "OCCURRED_AT", direction = Relationship.Direction.INCOMING)
    private Set<Event> events = new HashSet<>();

    public Location() {
        super();
    }

    public Location(String gameId, Patch p) {
        super(gameId, p);
    }

    @Override
    public Location merge(Patch p) {
        super.merge(p);
        return this;
    }

    @JsonIgnore
    public Set<Event> getEvents() {
        return Collections.unmodifiableSet(events);
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
}
