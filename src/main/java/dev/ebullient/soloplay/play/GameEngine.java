package dev.ebullient.soloplay.play;

import java.time.Instant;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.play.GameState.GamePhase;

@ApplicationScoped
public class GameEngine {

    @Inject
    GameRepository gameRepository;

    @Inject
    ActorCreationEngine actorCreationEngine;

    public GameResponse processRequest(String gameId, String playerInput, GameEventEmitter emitter) {
        Objects.requireNonNull(emitter, "emitter");
        GameState game = gameRepository.getOrCreateGameById(gameId);

        GamePhase phase = game.getGamePhase();
        boolean createActors = phase == GamePhase.CHARACTER_CREATION
                || (phase == GamePhase.UNKNOWN && !gameRepository.hasProtagonists(gameId));

        String trimmed = playerInput == null ? "" : playerInput.trim();
        if (isHelpCommand(trimmed)) {
            // Help responses do not change game state
            if (createActors) {
                GameResponse response = actorCreationEngine.help(game);
                return response;
            }
            GameResponse response = GameResponse.reply("""
                    Available commands:

                    - `/help` (or `help`, `?`): show commands
                    """);
            return response;
        }

        GameResponse response = createActors
                ? actorCreationEngine.processRequest(game, playerInput, emitter)
                : GameResponse.reply("Character creation is complete, but the next phase routing isn't implemented yet.");

        game.setLastPlayedAt(Instant.now());
        gameRepository.saveGame(game);
        return response;
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
