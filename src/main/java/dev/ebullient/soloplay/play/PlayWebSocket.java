package dev.ebullient.soloplay.play;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.ai.MarkdownAugmenter;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * WebSocket endpoint for streaming Play interactions.
 *
 * Path: /ws/play/{gameId}
 *
 * Provides real-time streaming of assistant responses with token-by-token
 * delivery.
 */
@WebSocket(path = "/ws/play/{gameId}")
public class PlayWebSocket {
    static final int MAX_HISTORY_MESSAGES = 250;

    // Simple in-memory history store per gameId.
    private static final Map<String, List<PlayWsServerMessage.HistoryMessage>> HISTORY = new ConcurrentHashMap<>();

    // Simple per-game connection counts to allow cleanup when the last client disconnects.
    private static final Map<String, Integer> ACTIVE_CONNECTIONS = new ConcurrentHashMap<>();
    /**
     * Shared generation locks per game.
     * Prevents concurrent generation across all connections to the same gameId.
     */
    private static final Map<String, AtomicBoolean> GENERATION_LOCKS = new ConcurrentHashMap<>();

    @Inject
    WebSocketConnection connection;

    @Inject
    OpenConnections openConnections;

    @Inject
    MarkdownAugmenter prettify;

    @Inject
    GameEngine gameEngine;

    @Inject
    GameRepository gameRepository;

    String gameId;

    /**
     * Called when a client connects to the WebSocket.
     */
    @OnOpen
    public Uni<PlayWsServerMessage> onOpen(@PathParam String gameId) {
        Log.infof("WebSocket connection opened (connection: %s, gameId: %s)", connection.id(), gameId);
        this.gameId = gameId;
        ACTIVE_CONNECTIONS.merge(gameId, 1, Integer::sum);

        // Ensure generation lock exists for this gameId
        GENERATION_LOCKS.computeIfAbsent(gameId, k -> new AtomicBoolean(false));

        return Uni.createFrom().item(new PlayWsServerMessage.Session(connection.id(), gameId));
    }

    /**
     * Called when a client disconnects.
     */
    @OnClose
    public void onClose() {
        Log.infof("WebSocket connection closed (connection: %s)", connection.id());

        if (gameId == null) {
            return;
        }

        int remaining = ACTIVE_CONNECTIONS.merge(gameId, -1, Integer::sum);
        if (remaining <= 0) {
            ACTIVE_CONNECTIONS.remove(gameId);
            HISTORY.remove(gameId);
            AtomicBoolean lock = GENERATION_LOCKS.remove(gameId);
            if (lock != null) {
                lock.set(false);
            }
        }
    }

    /**
     * Called when an error occurs during WebSocket processing.
     */
    @OnError
    public PlayWsServerMessage onError(Throwable error) {
        Log.errorf(error, "WebSocket error for connection: %s",
                connection.id());
        return new PlayWsServerMessage.Error(null, "Internal error: " + error.getMessage());
    }

    /**
     * Handles incoming client messages.
     * Routes to appropriate handler based on message type.
     */
    @OnTextMessage
    @RunOnVirtualThread
    public Multi<PlayWsServerMessage> onMessage(PlayWsClientMessage message) {
        if (message instanceof PlayWsClientMessage.HistoryRequest historyRequest) {
            return Multi.createFrom().item(history(historyRequest.limit()));
        }
        if (message instanceof PlayWsClientMessage.UserMessage userMessage) {
            return handleUserMessage(userMessage);
        }
        return Multi.createFrom().item(new PlayWsServerMessage.Error(null, "Unsupported message type"));
    }

    private PlayWsServerMessage.History history(int limit) {
        List<PlayWsServerMessage.HistoryMessage> allMessages = HISTORY.getOrDefault(gameId, List.of());
        if (allMessages.isEmpty()) {
            return new PlayWsServerMessage.History(List.of());
        }
        int start = Math.max(0, allMessages.size() - limit);
        return new PlayWsServerMessage.History(allMessages.subList(start, allMessages.size()));
    }

    private Multi<PlayWsServerMessage> handleUserMessage(PlayWsClientMessage.UserMessage userMessage) {
        String playerInput = userMessage.text();
        if (playerInput == null || playerInput.isBlank()) {
            return Multi.createFrom().item(new PlayWsServerMessage.Error(null, "Message text is required"));
        }

        // Enforce single in-flight generation per gameId
        AtomicBoolean lock = GENERATION_LOCKS.computeIfAbsent(gameId, k -> new AtomicBoolean(false));
        if (!lock.compareAndSet(false, true)) {
            return Multi.createFrom().item(
                    new PlayWsServerMessage.Error(null, "Generation already in progress for this game"));
        }

        String assistantId = UUID.randomUUID().toString();
        try {
            appendToHistory("user", playerInput);

            Log.infof("User message received (id: %s): %s", assistantId, truncate(playerInput, 100));

            broadcastToGameId(new PlayWsServerMessage.UserEcho(connection.id(), playerInput));
            broadcastToGameId(new PlayWsServerMessage.AssistantStart(assistantId));

            broadcastToGameId(new PlayWsServerMessage.AssistantDelta(assistantId, "middle"));
            var assistantMarkdown = "test";
            var assistantHtml = prettify.markdownToHtml(assistantMarkdown);

            broadcastToGameId(new PlayWsServerMessage.AssistantDone(assistantId, assistantMarkdown, assistantHtml));

            appendToHistory("assistant", assistantMarkdown, assistantHtml);
        } catch (Exception e) {
            Log.errorf(e, "Error handling user message for gameId: %s", gameId);
            broadcastToGameId(new PlayWsServerMessage.Error(assistantId, "Internal error: " + e.getMessage()));
        } finally {
            lock.set(false);
        }

        return Multi.createFrom().empty();
    }

    private void appendToHistory(String role, String markdown) {
        appendToHistory(role, markdown, prettify.markdownToHtml(markdown));
    }

    private void appendToHistory(String role, String markdown, String html) {
        if (gameId == null) {
            return;
        }
        HISTORY.compute(gameId, (k, messages) -> {
            List<PlayWsServerMessage.HistoryMessage> list = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
            list.add(new PlayWsServerMessage.HistoryMessage(role, markdown, html, Instant.now()));
            if (list.size() > MAX_HISTORY_MESSAGES) {
                list = list.subList(list.size() - MAX_HISTORY_MESSAGES, list.size());
            }
            return list.isEmpty() ? List.of() : List.copyOf(list);
        });
    }

    /**
     * sendAndAwait to preserve order when broadcast
     *
     * @param broadcaster
     * @param messages
     */
    private void broadcastToGameId(PlayWsServerMessage message) {
        String endpointId = connection.endpointId();
        openConnections.stream()
                .filter(c -> endpointId.equals(c.endpointId()))
                .filter(c -> gameId.equals(c.pathParam("gameId")))
                .forEach(c -> {
                    c.sendTextAndAwait(message);
                });
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
