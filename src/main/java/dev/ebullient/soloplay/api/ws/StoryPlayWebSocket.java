package dev.ebullient.soloplay.api.ws;

import java.util.List;
import java.util.UUID;
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
import dev.ebullient.soloplay.data.ConversationMessage;
import dev.ebullient.soloplay.data.StoryThread;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
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
 * Enforces single in-flight generation per connection.
 */
@WebSocket(path = "/ws/story/{storyThreadId}")
public class StoryPlayWebSocket {
    private static final Logger LOG = Logger.getLogger(StoryPlayWebSocket.class);

    @Inject
    StoryRepository storyRepository;

    @Inject
    GameMasterService gameMaster;

    @Inject
    WebSocketConnection connection;

    /** Tracks whether a generation is currently in progress for this connection. */
    private final AtomicBoolean generationInProgress = new AtomicBoolean(false);

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
     * Enforces single in-flight generation per connection.
     */
    private Multi<PlayWsServerMessage> handleUserMessage(PlayWsClientMessage.UserMessage message) {
        // Enforce single in-flight generation
        if (!generationInProgress.compareAndSet(false, true)) {
            return Multi.createFrom().item(
                    new PlayWsServerMessage.Error(null, "Generation already in progress"));
        }

        String messageId = UUID.randomUUID().toString();
        String threadId = storyThread.getId();
        LOG.infof("User message received (id: %s): %s", messageId, truncate(message.text(), 100));

        // Persist user message to transcript
        storyRepository.addConversationMessage(threadId, "user", message.text(), null);

        // Start streaming chat
        GameMasterService.StreamingChatResult result = gameMaster.chatStream(threadId, message.text());

        if (result == null) {
            generationInProgress.set(false);
            return Multi.createFrom().item(
                    new PlayWsServerMessage.Error(messageId, "Story thread not found"));
        }

        // Collect tokens for final markdown
        StringBuilder markdownBuilder = new StringBuilder();

        // Transform token stream into WebSocket messages
        Multi<PlayWsServerMessage> tokenMessages = result.tokenStream()
                .onItem().transform(token -> {
                    markdownBuilder.append(token);
                    return (PlayWsServerMessage) new AssistantDelta(messageId, token);
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.errorf(error, "Error during streaming (id: %s)", messageId);
                    generationInProgress.set(false);
                    return new PlayWsServerMessage.Error(messageId, "Error: " + error.getMessage());
                });

        // Create done message as a deferred Multi (evaluated on completion)
        Multi<PlayWsServerMessage> doneMessage = Multi.createFrom().deferred(() -> {
            String markdown = markdownBuilder.toString();
            String html = gameMaster.markdownToHtml(markdown);

            // Persist assistant message to transcript
            storyRepository.addConversationMessage(threadId, "assistant", markdown, html);

            gameMaster.updateLastPlayed(threadId);
            generationInProgress.set(false);
            LOG.debugf("Streaming complete (id: %s), markdown length: %d", messageId, markdown.length());
            return Multi.createFrom().item(new AssistantDone(messageId, markdown, html));
        });

        // Concatenate: start -> deltas -> done
        return Multi.createBy().concatenating().streams(
                Multi.createFrom().item((PlayWsServerMessage) new AssistantStart(messageId)),
                tokenMessages,
                doneMessage);
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
