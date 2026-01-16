package dev.ebullient.soloplay;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.neo4j.ogm.session.SessionFactory;

import dev.ebullient.soloplay.play.GameState;

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
                SET g.lastPlayedAt = timestamp()
                RETURN g
                """;
        return session.queryForObject(GameState.class, cypher, Map.of(
                "gameId", gameId,
                "gamePhase", GameState.GamePhase.CHARACTER_CREATION.name()));
    }

}
