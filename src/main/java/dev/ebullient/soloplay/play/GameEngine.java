package dev.ebullient.soloplay.play;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.play.model.GameState;
import dev.ebullient.soloplay.play.model.GameState.GamePhase;

@ApplicationScoped
public class GameEngine {

    @Inject
    GameRepository gameRepository;

    @Inject
    ActorCreationEngine actorCreationEngine;

    @Inject
    RollHandler rollHandler;

    public GameState getGameState(String gameId) {
        return gameRepository.findGameById(gameId);
    }

    public GameResponse processRequest(GameState game, String playerInput, GameEventEmitter emitter) {
        Objects.requireNonNull(emitter, "emitter");

        GamePhase phase = game.getGamePhase();
        boolean createActors = phase == GamePhase.CHARACTER_CREATION
                || (phase == GamePhase.UNKNOWN && !gameRepository.hasProtagonists(game.getGameId()))
                || "/newcharacter".equals(playerInput.trim());

        String trimmed = playerInput == null ? "" : playerInput.trim();
        if (isHelpCommand(trimmed)) {
            return handleHelpCommand(game, createActors);
        }

        final GameResponse response;
        if (createActors) {
            response = actorCreationEngine.processRequest(game, playerInput, emitter);
        } else if (game.getGamePhase() == GamePhase.SCENE_INITIALIZATION) {
            // This is not a turn: either a recap or session 0
            emitter.assistantDelta("Looking up recent story events…\n");

            response = GameResponse.reply("Not quite here yet: check for events (recap) or start new");
        } else {

            response = GameResponse
                    .reply("This phase of routing isn't implemented yet.");

            // game.incrementTurn();
        }

        gameRepository.saveGame(game);
        return response;
    }

    GameResponse handleHelpCommand(GameState game, boolean createActors) {
        // Help responses do not change game state
        if (createActors) {
            return actorCreationEngine.help(game);
        }
        return GameResponse.reply("""
                Available commands:

                - `/newcharacter`: create new player character
                - `/roll`: roll dice or enter roll result
                - `/start`: start or resume play
                - `/help` (or `help`, `?`): show commands
                """);
    }

    static boolean isHelpCommand(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return input.equalsIgnoreCase("/help")
                || input.equalsIgnoreCase("help")
                || input.equals("?");
    }
}
