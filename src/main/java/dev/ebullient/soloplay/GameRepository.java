package dev.ebullient.soloplay;

import static dev.ebullient.soloplay.StringUtils.normalize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

import dev.ebullient.soloplay.play.model.Actor;
import dev.ebullient.soloplay.play.model.Event;
import dev.ebullient.soloplay.play.model.GameState;
import dev.ebullient.soloplay.play.model.Location;
import dev.ebullient.soloplay.play.model.PlayerActor;

@ApplicationScoped
public class GameRepository {

    @Inject
    SessionFactory sessionFactory;

    private Map<String, List<Actor>> partyCache = new java.util.concurrent.ConcurrentHashMap<>();
    private Map<String, List<PlayerActor>> playerActorCache = new java.util.concurrent.ConcurrentHashMap<>();

    // ========= GAME ===============

    public GameState findGameById(String gameId) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (g:Game {gameId: $gameId})
                RETURN g
                """;
        return session.queryForObject(GameState.class, cypher, Map.of("gameId", gameId));
    }

    public GameState getOrCreateGameById(String gameId) {
        var session = sessionFactory.openSession();
        String cypher = """
                MERGE (g:Game {gameId: $gameId})
                ON CREATE SET g.gamePhase = $gamePhase
                RETURN g
                """;
        return session.queryForObject(GameState.class, cypher, Map.of(
                "gameId", gameId,
                "gamePhase", GameState.GamePhase.CHARACTER_CREATION.name()));
    }

    public GameState createGame(String gameId, String adventureName) {
        GameState game = new GameState();
        game.setGameId(gameId);
        game.setAdventureName(adventureName);
        game.setGamePhase(GameState.GamePhase.CHARACTER_CREATION);
        saveGame(game);
        return game;
    }

    public void saveGame(GameState game) {
        var session = sessionFactory.openSession();
        try (Transaction tx = session.beginTransaction()) {
            session.save(game);
            tx.commit();
            game.markClean();
        }
    }

    public List<GameState> listGames() {
        var session = sessionFactory.openSession();
        return new ArrayList<>(session.loadAll(GameState.class));
    }

    public void deleteGame(String gameId) {
        var session = sessionFactory.openSession();
        try (Transaction tx = session.beginTransaction()) {
            // Delete all nodes related to this game
            String cypher = """
                    MATCH (n {gameId: $gameId})
                    DETACH DELETE n
                    """;
            session.query(cypher, Map.of("gameId", gameId));
            tx.commit();
        }
    }

    // ========= ACTORS ===============

    public List<Actor> findTheParty(String gameId) {
        return partyCache.computeIfAbsent(gameId, this::loadTheParty);
    }

    public List<Actor> refreshTheParty(String gameId) {
        // clear caches (refreshParty command)
        partyCache.remove(gameId);
        playerActorCache.remove(gameId);
        return findTheParty(gameId);
    }

    private List<Actor> loadTheParty(String gameId) {
        var session = sessionFactory.openSession();
        // PlayerActors + Actors tagged as "party" or "player-controlled"
        String cypher = """
                MATCH (a {gameId: $gameId})
                WHERE a:PlayerActor OR (a:Actor AND ('party' IN a.tags OR 'player-controlled' IN a.tags))
                RETURN a
                """;
        Iterable<Actor> result = session.query(Actor.class, cypher, Map.of("gameId", gameId));
        List<Actor> party = new ArrayList<>();
        result.forEach(party::add);
        return party;
    }

    public List<PlayerActor> listPlayerActors(String gameId) {
        return playerActorCache.computeIfAbsent(gameId, k -> {
            var session = sessionFactory.openSession();
            String cypher = """
                    MATCH (a:PlayerActor {gameId: $gameId})
                    RETURN a
                    """;
            Iterable<PlayerActor> result = session.query(PlayerActor.class, cypher, Map.of("gameId", gameId));
            List<PlayerActor> actors = new ArrayList<>();
            result.forEach(actors::add);
            return actors;
        });
    }

    public boolean hasProtagonists(String gameId) {
        var actors = listPlayerActors(gameId);
        return actors == null ? false : actors.isEmpty();
    }

    public List<Actor> listActors(String gameId) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (a:Actor {gameId: $gameId})
                RETURN a
                """;
        Iterable<Actor> result = session.query(Actor.class, cypher, Map.of("gameId", gameId));
        List<Actor> actors = new ArrayList<>();
        result.forEach(actors::add);
        return actors;
    }

    public void saveActor(Actor actor) {
        var session = sessionFactory.openSession();
        try (Transaction tx = session.beginTransaction()) {
            session.save(actor);
            tx.commit();
            actor.markClean();
        }
    }

    public Actor findActorByNameOrAlias(String gameId, String nameOrAlias) {
        var session = sessionFactory.openSession();
        String normalized = normalize(nameOrAlias);
        String cypher = """
                MATCH (a:Actor {gameId: $gameId})
                WHERE toLower(a.name) = $name OR $name IN a.aliases
                RETURN a
                LIMIT 1
                """;
        return session.queryForObject(Actor.class, cypher, Map.of("gameId", gameId, "name", normalized));
    }

    public List<Actor> findActorsByTag(String gameId, String tag) {
        var session = sessionFactory.openSession();
        String normalized = normalize(tag);
        String cypher = """
                MATCH (a:Actor {gameId: $gameId})
                WHERE $tag IN a.tags
                RETURN a
                """;
        Iterable<Actor> result = session.query(Actor.class, cypher, Map.of("gameId", gameId, "tag", normalized));
        List<Actor> actors = new ArrayList<>();
        result.forEach(actors::add);
        return actors;
    }

    // ========= LOCATIONS ===============

    public List<Location> listLocations(String gameId) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (l:Location {gameId: $gameId})
                RETURN l
                """;
        Iterable<Location> result = session.query(Location.class, cypher, Map.of("gameId", gameId));
        List<Location> locations = new ArrayList<>();
        result.forEach(locations::add);
        return locations;
    }

    public Location findLocationByNameOrAlias(String gameId, String nameOrAlias) {
        var session = sessionFactory.openSession();
        String normalized = normalize(nameOrAlias);
        String cypher = """
                MATCH (l:Location {gameId: $gameId})
                WHERE toLower(l.name) = $name OR $name IN l.aliases
                RETURN l
                LIMIT 1
                """;
        return session.queryForObject(Location.class, cypher, Map.of("gameId", gameId, "name", normalized));
    }

    public List<Location> findLocationsByTag(String gameId, String tag) {
        var session = sessionFactory.openSession();
        String normalized = normalize(tag);
        String cypher = """
                MATCH (l:Location {gameId: $gameId})
                WHERE $tag IN l.tags
                RETURN l
                """;
        Iterable<Location> result = session.query(Location.class, cypher, Map.of("gameId", gameId, "tag", normalized));
        List<Location> locations = new ArrayList<>();
        result.forEach(locations::add);
        return locations;
    }

    // ========= EVENTS ===============

    public List<Event> listEvents(String gameId) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (e:Event {gameId: $gameId})
                RETURN e
                ORDER BY e.turnNumber
                """;
        Iterable<Event> result = session.query(Event.class, cypher, Map.of("gameId", gameId));
        List<Event> events = new ArrayList<>();
        result.forEach(events::add);
        return events;
    }

    public List<Event> findEventsByTag(String gameId, String tag) {
        var session = sessionFactory.openSession();
        String normalized = normalize(tag);
        String cypher = """
                MATCH (e:Event {gameId: $gameId})
                WHERE $tag IN e.tags
                RETURN e
                ORDER BY e.turnNumber
                """;
        Iterable<Event> result = session.query(Event.class, cypher, Map.of("gameId", gameId, "tag", normalized));
        List<Event> events = new ArrayList<>();
        result.forEach(events::add);
        return events;
    }
}
