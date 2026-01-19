package dev.ebullient.soloplay.play.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@NodeEntity
public class Event extends BaseEntity {

    @CheckedTemplate(basePath = "models")
    public static class Templates {
        public static native TemplateInstance eventDetail(Event event);
    }

    @Id
    private String id; // gameId:event-{timestamp}-{turnNumber}
    private String gameId;
    private String summary; // Brief description of what happened (from turnSummary)
    private Integer turnNumber; // Game turn when this occurred

    @Relationship(type = "PARTICIPATED_IN", direction = Relationship.Direction.INCOMING)
    private Set<Actor> participants;

    @Relationship(type = "OCCURRED_AT", direction = Relationship.Direction.OUTGOING)
    private Set<Location> locations;

    public Event() {
        super();
        this.participants = new HashSet<>();
        this.locations = new HashSet<>();
    }

    public Event(String gameId, Integer turnNumber, String summary) {
        this();
        this.gameId = gameId;
        this.turnNumber = turnNumber;
        this.summary = summary;
        this.id = gameId + ":event-" + createdAt + "-" + turnNumber;
    }

    public String getId() {
        return id;
    }

    public String getGameId() {
        return gameId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
        markDirty();
    }

    public Integer getTurnNumber() {
        return turnNumber;
    }

    public Set<Actor> getParticipants() {
        return participants;
    }

    public void addParticipant(Actor actor) {
        if (participants.add(actor)) {
            actor.getEvents().add(this);
            markDirty();
        }
    }

    public void removeParticipant(Actor actor) {
        if (participants.remove(actor)) {
            actor.getEvents().remove(this);
            markDirty();
        }
    }

    public Set<Location> getLocations() {
        return locations;
    }

    public void addLocation(Location location) {
        if (locations.add(location)) {
            location.getEvents().add(this);
            markDirty();
        }
    }

    public void removeLocation(Location location) {
        if (locations.remove(location)) {
            location.getEvents().remove(this);
            markDirty();
        }
    }

    public String render() {
        return Templates.eventDetail(this).render();
    }
}
