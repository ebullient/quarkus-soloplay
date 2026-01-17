package dev.ebullient.soloplay.api;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestPath;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.play.model.GameState;

/**
 * REST API for game management operations.
 */
@ApplicationScoped
@Path("/api/game")
public class GameResource {

    @Inject
    GameRepository gameRepository;

    /**
     * List all games.
     *
     * @return List of all games
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<GameState> list() {
        return gameRepository.listGames();
    }

    /**
     * Get a specific game by ID.
     *
     * @param gameId The game identifier
     * @return The game state, or 404 if not found
     */
    @GET
    @Path("/{gameId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@RestPath String gameId) {
        GameState game = gameRepository.findGameById(gameId);
        if (game == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(game).build();
    }

    /**
     * Delete a game and all its related resources.
     *
     * @param gameId The game identifier
     * @return 204 No Content on success
     */
    @DELETE
    @Path("/{gameId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@RestPath String gameId) {
        gameRepository.deleteGame(gameId);
        return Response.noContent().build();
    }
}
