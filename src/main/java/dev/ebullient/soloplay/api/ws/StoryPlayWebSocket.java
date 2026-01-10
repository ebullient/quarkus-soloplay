package dev.ebullient.soloplay.api.ws;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.ai.GameMasterService;
import dev.ebullient.soloplay.api.ws.PlayWsServerMessage.AssistantDelta;
import dev.ebullient.soloplay.api.ws.PlayWsServerMessage.AssistantDone;
import dev.ebullient.soloplay.api.ws.PlayWsServerMessage.AssistantStart;
import dev.ebullient.soloplay.api.ws.PlayWsServerMessage.History;
import dev.ebullient.soloplay.api.ws.PlayWsServerMessage.Session;
import dev.ebullient.soloplay.api.ws.PlayWsServerMessage.UserEcho;
import dev.ebullient.soloplay.data.ConversationMessage;
import dev.ebullient.soloplay.data.StoryThread;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * WebSocket endpoint for streaming Play interactions.
 *
 * Path: /ws/story/{storyThreadId}
 *
 * Provides real-time streaming of GM responses with token-by-token delivery.
 * Broadcasts messages to all connections watching the same story thread,
 * enabling multi-tab and limited multiplayer support.
 *
 * Enforces single in-flight generation per story thread (not per connection).
 */
@WebSocket(path = "/ws/story/{storyThreadId}")
public class StoryPlayWebSocket {
    private static final Logger LOG = Logger.getLogger(StoryPlayWebSocket.class);

    /**
     * Shared generation locks per story thread.
     * Prevents concurrent generation across all connections to the same story.
     */
    private static final ConcurrentHashMap<String, AtomicBoolean> GENERATION_LOCKS = new ConcurrentHashMap<>();

    @Inject
    StoryRepository storyRepository;

    @Inject
    GameMasterService gameMaster;

    @Inject
    WebSocketConnection connection;

    @Inject
    OpenConnections openConnections;

    /** Cached story thread for this connection (loaded on open). */
    private StoryThread storyThread;

    /**
     * Called when a client connects to the WebSocket.
     * Validates the story thread exists and sends session info.
     */
    @OnOpen
    public Uni<PlayWsServerMessage> onOpen(@PathParam String storyThreadId) {
        LOG.infof("WebSocket connection opened for story thread: %s (connection: %s)",
                storyThreadId, connection.id());

        storyThread = storyRepository.findStoryThreadById(storyThreadId);

        if (storyThread == null) {
            LOG.warnf("Story thread not found: %s", storyThreadId);
            return Uni.createFrom().item(
                    new PlayWsServerMessage.Error(null, "Story thread not found: " + storyThreadId));
        }

        // Ensure generation lock exists for this story thread
        GENERATION_LOCKS.putIfAbsent(storyThreadId, new AtomicBoolean(false));

        return Uni.createFrom().item(
                new Session(storyThread.getId(), storyThread.getName()));
    }

    /**
     * Called when a client disconnects.
     */
    @OnClose
    public void onClose(@PathParam String storyThreadId) {
        LOG.infof("WebSocket connection closed for story thread: %s (connection: %s)",
                storyThreadId, connection.id());
        // Note: We don't remove the generation lock on close - other connections may still be using it
    }

    /**
     * Called when an error occurs during WebSocket processing.
     */
    @OnError
    public PlayWsServerMessage onError(@PathParam String storyThreadId, Throwable error) {
        LOG.errorf(error, "WebSocket error for story thread: %s (connection: %s)",
                storyThreadId, connection.id());
        return new PlayWsServerMessage.Error(null, "Internal error: " + error.getMessage());
    }

    /**
     * Handles incoming client messages.
     * Routes to appropriate handler based on message type.
     */
    @OnTextMessage
    public Multi<PlayWsServerMessage> onMessage(PlayWsClientMessage message) {
        if (storyThread == null) {
            return Multi.createFrom().item(
                    new PlayWsServerMessage.Error(null, "Not connected to a valid story thread"));
        }

        return switch (message) {
            case PlayWsClientMessage.HistoryRequest req -> handleHistoryRequest(req);
            case PlayWsClientMessage.UserMessage msg -> handleUserMessage(msg);
        };
    }

    /**
     * Handle history request.
     * Returns conversation transcript from persistent storage.
     * History is per-connection (not broadcast).
     */
    private Multi<PlayWsServerMessage> handleHistoryRequest(PlayWsClientMessage.HistoryRequest request) {
        int limit = request.limit() != null ? request.limit() : 50;
        LOG.debugf("History request received (limit: %d)", limit);

        List<ConversationMessage> messages = storyRepository.getConversationHistory(
                storyThread.getId(), limit);

        List<PlayWsServerMessage.HistoryMessage> historyMessages = messages.stream()
                .map(m -> new PlayWsServerMessage.HistoryMessage(
                        m.getRole(),
                        m.getMarkdown(),
                        m.getHtml(),
                        m.getTimestamp()))
                .toList();

        return Multi.createFrom().item(new History(historyMessages));
    }

    /**
     * Handle user message - invoke GM and stream response.
     * Persists both user and assistant messages to transcript.
     * Broadcasts all messages to all connections watching this story thread.
     * Enforces single in-flight generation per story thread.
     */
    private Multi<PlayWsServerMessage> handleUserMessage(PlayWsClientMessage.UserMessage message) {
        String threadId = storyThread.getId();

        // Get shared generation lock for this story thread
        AtomicBoolean generationLock = GENERATION_LOCKS.computeIfAbsent(threadId, k -> new AtomicBoolean(false));

        // Enforce single in-flight generation per story thread
        if (!generationLock.compareAndSet(false, true)) {
            return Multi.createFrom().item(
                    new PlayWsServerMessage.Error(null, "Generation already in progress for this story"));
        }

        String messageId = UUID.randomUUID().toString();
        LOG.infof("User message received (id: %s): %s", messageId, truncate(message.text(), 100));

        // Persist user message to transcript
        storyRepository.addConversationMessage(threadId, "user", message.text(), null);

        // Broadcast user message to all connections (so other tabs see the input)
        broadcastToStoryThread(threadId, new UserEcho(message.text()));

        // Start streaming chat
        GameMasterService.StreamingChatResult result = gameMaster.chatStream(threadId, message.text());

        if (result == null) {
            generationLock.set(false);
            return Multi.createFrom().item(
                    new PlayWsServerMessage.Error(messageId, "Story thread not found"));
        }

        // Collect tokens for final markdown
        StringBuilder markdownBuilder = new StringBuilder();

        // Track whether streaming succeeded (don't persist on failure)
        AtomicBoolean streamSucceeded = new AtomicBoolean(true);

        // Broadcast start message to all connections
        broadcastToStoryThread(threadId, new AssistantStart(messageId));

        // Transform token stream into WebSocket messages
        // Each token is broadcast to all connections
        Multi<PlayWsServerMessage> tokenMessages = result.tokenStream()
                .onItem().invoke(token -> {
                    markdownBuilder.append(token);
                    // Broadcast delta to all connections watching this story
                    broadcastToStoryThread(threadId, new AssistantDelta(messageId, token));
                })
                .onItem().transform(token -> (PlayWsServerMessage) new AssistantDelta(messageId, token))
                .onFailure().invoke(error -> {
                    LOG.errorf(error, "Error during streaming (id: %s)", messageId);
                    streamSucceeded.set(false);
                    generationLock.set(false);
                    // Broadcast error to all connections
                    broadcastToStoryThread(threadId,
                            new PlayWsServerMessage.Error(messageId, "Error: " + error.getMessage()));
                })
                .onFailure().recoverWithItem(error -> new PlayWsServerMessage.Error(messageId, "Error: " + error.getMessage()));

        // Create done message as a deferred Multi (evaluated on completion)
        // Skip persistence and done message on failure
        Multi<PlayWsServerMessage> doneMessage = Multi.createFrom().deferred(() -> {
            if (!streamSucceeded.get()) {
                return Multi.createFrom().empty();
            }

            String markdown = markdownBuilder.toString();
            String html = gameMaster.markdownToHtml(markdown);

            // Persist assistant message to transcript
            storyRepository.addConversationMessage(threadId, "assistant", markdown, html);

            gameMaster.updateLastPlayed(threadId);
            generationLock.set(false);
            LOG.debugf("Streaming complete (id: %s), markdown length: %d", messageId, markdown.length());

            // Broadcast done message to all connections
            AssistantDone doneMsg = new AssistantDone(messageId, markdown, html);
            broadcastToStoryThread(threadId, doneMsg);

            return Multi.createFrom().item(doneMsg);
        });

        // Return empty Multi for the originating connection since we're broadcasting
        // The originating connection will receive messages via broadcast like everyone else
        return Multi.createBy().concatenating().streams(
                tokenMessages.onItem().transform(msg -> (PlayWsServerMessage) null).filter(msg -> false),
                doneMessage.onItem().transform(msg -> (PlayWsServerMessage) null).filter(msg -> false));
    }

    /**
     * Broadcast a message to all connections watching the specified story thread.
     */
    private void broadcastToStoryThread(String storyThreadId, PlayWsServerMessage message) {
        openConnections.stream()
                .filter(conn -> conn.pathParam("storyThreadId").equals(storyThreadId))
                .forEach(conn -> conn.sendText(message).subscribe().with(
                        success -> {
                        },
                        error -> LOG.warnf("Failed to send to connection %s: %s", conn.id(), error.getMessage())));
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
