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
public class Actor extends NamedBaseEntity {

    @CheckedTemplate(basePath = "models")
    public static class Templates {
        public static native TemplateInstance actorDetail(Actor actor);

        public static native TemplateInstance actorSummary(Actor actor);
    }

    @Relationship(type = "PARTICIPATED_IN", direction = Relationship.Direction.OUTGOING)
    protected Set<Event> events = new HashSet<>();

    public Actor() {
        super();
    }

    public Actor(String gameId, Patch p) {
        super(gameId, p);
    }

    @Override
    public Actor merge(Patch p) {
        super.merge(p);
        var patchSources = p.sources();
        if (patchSources != null && !patchSources.isEmpty()) {
            getSources().addAll(patchSources);
        }
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
        return Templates.actorDetail(this).render();
    }
}
