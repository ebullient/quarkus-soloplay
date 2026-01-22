package dev.ebullient.soloplay.play;

import static dev.ebullient.soloplay.StringUtils.valueOrPlaceholder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.play.model.GameState;
import dev.ebullient.soloplay.play.model.GameState.GamePhase;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkus.logging.Log;

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
        gameContext.setGameState(game, gamePlayEngine.listTheParty(game));

        GamePhase phase = game.getGamePhase();
        Log.debugf("Resuming game phase: %s", phase);

        String trimmed = playerInput == null ? "" : playerInput.trim();

        // RECOVERY: normalize UNKNOWN into a concrete phase
        if (phase == GamePhase.UNKNOWN) {
            if (gameRepository.hasProtagonists(game.getGameId())) {
                Log.infof("Recovering game %s from UNKNOWN to ACTIVE_PLAY (protagonists exist)", game.getGameId());
                game.setGamePhase(GamePhase.ACTIVE_PLAY);
                phase = GamePhase.ACTIVE_PLAY;
            } else {
                Log.infof("Recovering game %s from UNKNOWN to CHARACTER_CREATION (no protagonists)", game.getGameId());
                game.setGamePhase(GamePhase.CHARACTER_CREATION);
                phase = GamePhase.CHARACTER_CREATION;
            }
        }

        boolean createActors = phase == GamePhase.CHARACTER_CREATION || "/newcharacter".equals(trimmed);
        if (isStatusCommand(trimmed)) {
            return handleStatusCommand(game);
        }
        if (isHelpCommand(trimmed)) {
            return handleHelpCommand(game, createActors);
        }

        final GameResponse response;
        if (createActors) {
            response = actorCreationEngine.processRequest(game, playerInput, emitter);
            gameRepository.refreshTheParty(game.getGameId());
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
            game.setGamePhase(game.getGamePhase().next());
        } else {
            response = gamePlayEngine.processRequest(game, playerInput, emitter);
            game.incrementTurn();
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
                - `/status`: show game state information
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

    static boolean isStatusCommand(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return input.equalsIgnoreCase("/status");
    }

    GameResponse handleStatusCommand(GameState game) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Game Status**\n\n");
        sb.append("- **Phase**: ").append(game.getGamePhase().name()).append("\n");
        sb.append("- **Turn**: ").append(valueOrPlaceholder(game.getTurnNumber())).append("\n");
        sb.append("- **Adventure**: ").append(valueOrPlaceholder(game.getAdventureName())).append("\n");
        sb.append("- **Location**: ").append(valueOrPlaceholder(game.getCurrentLocation())).append("\n");
        sb.append("- **Last Played**: ").append(formatLastPlayed(game.getLastPlayedAt())).append("\n");

        var party = gameRepository.findTheParty(game.getGameId());
        if (!party.isEmpty()) {
            sb.append("\n**Party Members**\n\n");
            for (var member : party) {
                sb.append("- **").append(member.getName()).append("**");
                if (member instanceof dev.ebullient.soloplay.play.model.PlayerActor pa) {
                    if (pa.getActorClass() != null) {
                        sb.append(" (").append(pa.getActorClass());
                        if (pa.getLevel() != null) {
                            sb.append(" ").append(pa.getLevel());
                        }
                        sb.append(")");
                    }
                }
                if (member.getSummary() != null) {
                    sb.append(": ").append(member.getSummary());
                }
                sb.append("\n");
            }
        }

        return GameResponse.reply(sb.toString());
    }

    private String formatLastPlayed(Long epochMillis) {
        if (epochMillis == null) {
            return "—";
        }
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
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
