package dev.ebullient.soloplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

import dev.ebullient.soloplay.play.model.Actor;
import dev.ebullient.soloplay.play.model.GameState;

@ApplicationScoped
public class GameRepository {

    @Inject
    SessionFactory sessionFactory;

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

    public boolean hasProtagonists(String gameId) {
        var session = sessionFactory.openSession();
        String cypher = """
                MATCH (a:PlayerActor {gameId: $gameId})
                RETURN count(a) as c
                """;
        Long count = session.queryForObject(Long.class, cypher, Map.of("gameId", gameId));
        return count != null && count > 0;
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
}
