package dev.ebullient.soloplay.api.ws;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

import dev.ebullient.soloplay.ai.MarkdownAugmenter;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * WebSocket endpoint for streaming Play interactions.
 *
 * Path: /ws/play
 *
 * Provides real-time streaming of assistant responses with token-by-token delivery.
 */
@WebSocket(path = "/ws/play")
public class PlayWebSocket {
    static final String DEFAULT_SESSION_NAME = "Solo Play";

    // Simple in-memory history store keyed by connection id.
    // This is intentionally minimal so the websocket works even if story support is removed.
    private static final Map<String, List<PlayWsServerMessage.HistoryMessage>> HISTORY_BY_CONNECTION = new ConcurrentHashMap<>();

    @Inject
    WebSocketConnection connection;

    @Inject
    OpenConnections openConnections;

    @Inject
    MarkdownAugmenter prettify;

    /**
     * Called when a client connects to the WebSocket.
     */
    @OnOpen
    public Uni<PlayWsServerMessage> onOpen() {
        Log.infof("WebSocket connection opened (connection: %s)", connection.id());
        return Uni.createFrom().item(new PlayWsServerMessage.Session(connection.id(), DEFAULT_SESSION_NAME));
    }

    /**
     * Called when a client disconnects.
     */
    @OnClose
    public void onClose() {
        Log.infof("WebSocket connection closed (connection: %s)", connection.id());
        HISTORY_BY_CONNECTION.remove(connection.id());
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
    public Multi<PlayWsServerMessage> onMessage(PlayWsClientMessage message) {
        if (message instanceof PlayWsClientMessage.HistoryRequest historyRequest) {
            return Multi.createFrom().item(historyForConnection(historyRequest.limit()));
        }
        if (message instanceof PlayWsClientMessage.UserMessage userMessage) {
            return handleUserMessage(userMessage);
        }
        return Multi.createFrom().item(new PlayWsServerMessage.Error(null, "Unsupported message type"));
    }

    private PlayWsServerMessage.History historyForConnection(int limit) {
        List<PlayWsServerMessage.HistoryMessage> allMessages = HISTORY_BY_CONNECTION.getOrDefault(connection.id(), List.of());
        if (allMessages.isEmpty()) {
            return new PlayWsServerMessage.History(List.of());
        }
        int start = Math.max(0, allMessages.size() - limit);
        return new PlayWsServerMessage.History(allMessages.subList(start, allMessages.size()));
    }

    private Multi<PlayWsServerMessage> handleUserMessage(PlayWsClientMessage.UserMessage userMessage) {
        String text = userMessage.text();
        if (text == null || text.isBlank()) {
            return Multi.createFrom().item(new PlayWsServerMessage.Error(null, "Message text is required"));
        }

        appendToHistory("user", text);

        String assistantId = UUID.randomUUID().toString();
        String assistantMarkdown = generateAssistantMarkdown(text);
        String assistantHtml = prettify.markdownToHtml(assistantMarkdown);
        appendToHistory("assistant", assistantMarkdown, assistantHtml);

        List<PlayWsServerMessage> out = new ArrayList<>();
        out.add(new PlayWsServerMessage.UserEcho(text));
        out.add(new PlayWsServerMessage.AssistantStart(assistantId));
        for (String chunk : chunkForStreaming(assistantMarkdown, 24)) {
            out.add(new PlayWsServerMessage.AssistantDelta(assistantId, chunk));
        }
        out.add(new PlayWsServerMessage.AssistantDone(assistantId, assistantMarkdown, assistantHtml));
        return Multi.createFrom().iterable(out);
    }

    private void appendToHistory(String role, String markdown) {
        appendToHistory(role, markdown, prettify.markdownToHtml(markdown));
    }

    private void appendToHistory(String role, String markdown, String html) {
        HISTORY_BY_CONNECTION.compute(connection.id(), (id, messages) -> {
            List<PlayWsServerMessage.HistoryMessage> list = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
            list.add(new PlayWsServerMessage.HistoryMessage(role, markdown, html, Instant.now()));
            return List.copyOf(list);
        });
    }

    private String generateAssistantMarkdown(String userText) {
        return "You said: " + userText;
    }

    static List<String> chunkForStreaming(String text, int chunkSize) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        int size = Math.max(1, chunkSize);
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += size) {
            chunks.add(text.substring(i, Math.min(text.length(), i + size)));
        }
        return chunks;
    }
}
