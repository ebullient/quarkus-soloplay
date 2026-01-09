package dev.ebullient.soloplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.WebSocketClientConnection;

@QuarkusTest
class StoryPlayWebSocketTest {

    @TestHTTPResource("/ws/story/test-story")
    URI wsUri;

    @Inject
    BasicWebSocketConnector connector;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    StoryRepository storyRepository;

    @BeforeEach
    void setup() {
        // Ensure test story thread exists
        if (storyRepository.findStoryThreadById("test-story") == null) {
            storyRepository.createStoryThread("Test Story", null, null);
        }
    }

    @Test
    void testConnectionSendsSessionMessage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        WebSocketClientConnection connection = connector
                .baseUri(wsUri)
                .onTextMessage((c, msg) -> {
                    receivedMessage.set(msg);
                    latch.countDown();
                })
                .connectAndAwait();

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Should receive session message");

            String message = receivedMessage.get();
            assertNotNull(message);

            JsonNode json = objectMapper.readTree(message);
            assertEquals("session", json.get("type").asText());
            assertEquals("test-story", json.get("storyThreadId").asText());
            assertNotNull(json.get("storyName"));
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    void testInvalidStoryThreadReturnsError() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        // Connect to non-existent story thread
        URI invalidUri = URI.create(wsUri.toString().replace("test-story", "non-existent-story"));

        WebSocketClientConnection connection = connector
                .baseUri(invalidUri)
                .onTextMessage((c, msg) -> {
                    receivedMessage.set(msg);
                    latch.countDown();
                })
                .connectAndAwait();

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Should receive error message");

            String message = receivedMessage.get();
            assertNotNull(message);

            JsonNode json = objectMapper.readTree(message);
            assertEquals("error", json.get("type").asText());
            assertTrue(json.get("message").asText().contains("not found"));
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    void testHistoryRequestReturnsEmptyHistory() throws Exception {
        CountDownLatch sessionLatch = new CountDownLatch(1);
        CountDownLatch historyLatch = new CountDownLatch(1);
        AtomicReference<String> historyMessage = new AtomicReference<>();

        WebSocketClientConnection connection = connector
                .baseUri(wsUri)
                .onTextMessage((c, msg) -> {
                    try {
                        JsonNode json = objectMapper.readTree(msg);
                        String type = json.get("type").asText();
                        if ("session".equals(type)) {
                            sessionLatch.countDown();
                        } else if ("history".equals(type)) {
                            historyMessage.set(msg);
                            historyLatch.countDown();
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                })
                .connectAndAwait();

        try {
            // Wait for session message
            assertTrue(sessionLatch.await(5, TimeUnit.SECONDS), "Should receive session message");

            // Send history request
            connection.sendTextAndAwait("{\"type\":\"history_request\",\"limit\":100}");

            // Wait for history response
            assertTrue(historyLatch.await(5, TimeUnit.SECONDS), "Should receive history message");

            String message = historyMessage.get();
            assertNotNull(message);

            JsonNode json = objectMapper.readTree(message);
            assertEquals("history", json.get("type").asText());
            assertTrue(json.get("messages").isArray());
            assertEquals(0, json.get("messages").size()); // Empty until qsu.4
        } finally {
            connection.closeAndAwait();
        }
    }
}
