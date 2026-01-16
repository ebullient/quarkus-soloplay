package dev.ebullient.soloplay.play;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.play.model.GameState;
import dev.ebullient.soloplay.play.model.GameState.GamePhase;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

@ApplicationScoped
public class GameEngine {

    @Inject
    GameRepository gameRepository;

    @Inject
    ActorCreationEngine actorCreationEngine;

    @Inject
    RollHandler rollHandler;

    @Inject
    GamePlayEngine gamePlayEngine;

    @Inject
    ChatMemoryStore chatMemoryStore;

    @Inject
    GameContext gameContext;

    public GameState getGameState(String gameId) {
        return gameRepository.findGameById(gameId);
    }

    public GameResponse processRequest(GameState game, String playerInput, GameEventEmitter emitter, boolean resuming) {
        Objects.requireNonNull(emitter, "emitter");
        gameContext.setGameId(game.getGameId());

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
        } else if (game.getGamePhase() == GamePhase.SCENE_INITIALIZATION || resuming) {
            // Check for existing chat history to decide: recap or fresh start
            List<ChatMessage> chatHistory = chatMemoryStore.getMessages(game.getGameId());
            if (chatHistory.isEmpty()) {
                // New game - start the opening scene
                response = gamePlayEngine.sceneStart(game, emitter);
                game.incrementTurn();
            } else {
                // Resuming - build recap from chat history
                String recentEvents = formatRecentEvents(chatHistory);
                response = gamePlayEngine.recap(game, recentEvents, emitter);
            }
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

    /**
     * Format recent chat history into a summary for the recap prompt.
     * Takes the last few exchanges and formats them as a narrative summary.
     */
    String formatRecentEvents(List<ChatMessage> chatHistory) {
        if (chatHistory.isEmpty()) {
            return "No previous events.";
        }

        // Take last 10 messages (5 exchanges) for context
        int start = Math.max(0, chatHistory.size() - 10);
        List<ChatMessage> recent = chatHistory.subList(start, chatHistory.size());

        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : recent) {
            switch (msg.type()) {
                case USER -> sb.append("=== Player ===\n").append(extractText(msg)).append("\n\n");
                case AI -> sb.append("=== GM ===\n").append(extractText(msg)).append("\n\n");
                default -> {
                    /* skip system messages */ }
            }
        }
        return sb.toString().trim();
    }

    /**
     * Extract text content from a ChatMessage.
     */
    private String extractText(ChatMessage msg) {
        if (msg instanceof dev.langchain4j.data.message.AiMessage ai) {
            return ai.text() != null ? ai.text() : "";
        } else if (msg instanceof dev.langchain4j.data.message.UserMessage user) {
            return user.singleText();
        }
        return "";
    }
}
