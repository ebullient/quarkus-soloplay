package dev.ebullient.soloplay.play;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.play.model.Actor;
import dev.ebullient.soloplay.play.model.Event;
import dev.ebullient.soloplay.play.model.Location;
import dev.langchain4j.agent.tool.Tool;

/**
 * AI Tools for querying game state.
 * Provides access to actors, locations, events, and party information.
 */
@ApplicationScoped
public class GameTools {

    @Inject
    GameRepository gameRepository;

    @Inject
    GameContext gameContext;

    @Tool("""
            Find an actor (NPC or creature) by name or alias.
            Returns the actor's details if found, or a message indicating not found.
            Use this to check if an NPC already exists before creating a new one.
            """)
    public String findActor(String name) {
        String gameId = gameContext.getGameId();
        if (gameId == null) {
            return "Error: No game context available";
        }

        Actor actor = gameRepository.findActorByNameOrAlias(gameId, name);
        if (actor == null) {
            return "No actor found with name or alias: " + name;
        }
        return actor.render();
    }

    @Tool("""
            Find all actors with a specific tag (e.g., "hostile", "merchant", "quest-giver").
            Returns a list of matching actors, or empty if none found.
            """)
    public String findActorsByTag(String tag) {
        String gameId = gameContext.getGameId();
        if (gameId == null) {
            return "Error: No game context available";
        }

        List<Actor> actors = gameRepository.findActorsByTag(gameId, tag);
        if (actors.isEmpty()) {
            return "No actors found with tag: " + tag;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(actors.size()).append(" actor(s) with tag '").append(tag).append("':\n\n");
        for (Actor actor : actors) {
            sb.append(actor.render()).append("\n---\n");
        }
        return sb.toString();
    }

    @Tool("""
            Find a location by name or alias.
            Returns the location's details if found, or a message indicating not found.
            Use this to check if a location already exists before creating a new one.
            """)
    public String findLocation(String name) {
        String gameId = gameContext.getGameId();
        if (gameId == null) {
            return "Error: No game context available";
        }

        Location location = gameRepository.findLocationByNameOrAlias(gameId, name);
        if (location == null) {
            return "No location found with name or alias: " + name;
        }
        return location.render();
    }

    @Tool("""
            Find all locations with a specific tag (e.g., "tavern", "dungeon", "shop").
            Returns a list of matching locations, or empty if none found.
            """)
    public String findLocationsByTag(String tag) {
        String gameId = gameContext.getGameId();
        if (gameId == null) {
            return "Error: No game context available";
        }

        List<Location> locations = gameRepository.findLocationsByTag(gameId, tag);
        if (locations.isEmpty()) {
            return "No locations found with tag: " + tag;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(locations.size()).append(" location(s) with tag '").append(tag).append("':\n\n");
        for (Location location : locations) {
            sb.append(location.render()).append("\n---\n");
        }
        return sb.toString();
    }

    @Tool("""
            Get recent events from the game's history.
            Returns the most recent events ordered by turn number.
            Use to recall what has happened in the adventure.
            """)
    public String getRecentEvents(int count) {
        String gameId = gameContext.getGameId();
        if (gameId == null) {
            return "Error: No game context available";
        }

        List<Event> events = gameRepository.listEvents(gameId);
        if (events.isEmpty()) {
            return "No events recorded yet";
        }

        // Get the most recent events
        int start = Math.max(0, events.size() - count);
        List<Event> recent = events.subList(start, events.size());

        StringBuilder sb = new StringBuilder();
        sb.append("Recent events (").append(recent.size()).append("):\n\n");
        for (Event event : recent) {
            sb.append(event.render()).append("\n---\n");
        }
        return sb.toString();
    }

    @Tool("""
            Find all events with a specific tag (e.g., "combat", "discovery", "milestone").
            Returns matching events ordered by turn number.
            """)
    public String findEventsByTag(String tag) {
        String gameId = gameContext.getGameId();
        if (gameId == null) {
            return "Error: No game context available";
        }

        List<Event> events = gameRepository.findEventsByTag(gameId, tag);
        if (events.isEmpty()) {
            return "No events found with tag: " + tag;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(events.size()).append(" event(s) with tag '").append(tag).append("':\n\n");
        for (Event event : events) {
            sb.append(event.render()).append("\n---\n");
        }
        return sb.toString();
    }
}
