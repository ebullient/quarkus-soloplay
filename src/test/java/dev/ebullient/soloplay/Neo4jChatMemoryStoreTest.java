package dev.ebullient.soloplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.soloplay.ai.memory.Neo4jChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class Neo4jChatMemoryStoreTest {

    @Inject
    Neo4jChatMemoryStore memoryStore;

    @BeforeAll
    static void requireNeo4j() {
        Assumptions.assumeTrue(isNeo4jAvailable(),
                "Neo4j must be running on localhost:7687 to run " + Neo4jChatMemoryStoreTest.class.getSimpleName());
    }

    @Test
    void testStoreIsInjected() {
        assertNotNull(memoryStore);
    }

    @Test
    void testSaveAndRetrieveMessages() {
        String memoryId = "test-memory-" + System.currentTimeMillis();

        // Initially empty
        List<ChatMessage> initialMessages = memoryStore.getMessages(memoryId);
        assertTrue(initialMessages.isEmpty());

        // Save some messages
        List<ChatMessage> messages = List.of(
                UserMessage.from("Hello, I'm a brave adventurer"),
                AiMessage.from("Welcome, brave adventurer! What brings you to these lands?"),
                UserMessage.from("I seek the lost treasure of Phandelver"));

        memoryStore.updateMessages(memoryId, messages);

        // Retrieve and verify
        List<ChatMessage> retrieved = memoryStore.getMessages(memoryId);
        assertEquals(3, retrieved.size());

        // Verify message content
        assertTrue(retrieved.get(0) instanceof UserMessage);
        assertEquals("Hello, I'm a brave adventurer", ((UserMessage) retrieved.get(0)).singleText());

        assertTrue(retrieved.get(1) instanceof AiMessage);
        assertEquals("Welcome, brave adventurer! What brings you to these lands?",
                ((AiMessage) retrieved.get(1)).text());

        assertTrue(retrieved.get(2) instanceof UserMessage);
        assertEquals("I seek the lost treasure of Phandelver", ((UserMessage) retrieved.get(2)).singleText());

        // Cleanup
        memoryStore.deleteMessages(memoryId);

        // Verify deletion
        List<ChatMessage> afterDelete = memoryStore.getMessages(memoryId);
        assertTrue(afterDelete.isEmpty());
    }

    @Test
    void testUpdateReplacesMessages() {
        String memoryId = "test-update-" + System.currentTimeMillis();

        // Save initial messages
        memoryStore.updateMessages(memoryId, List.of(
                UserMessage.from("First message")));

        // Update with new messages (should replace)
        memoryStore.updateMessages(memoryId, List.of(
                UserMessage.from("First message"),
                AiMessage.from("Response to first"),
                UserMessage.from("Second message")));

        List<ChatMessage> retrieved = memoryStore.getMessages(memoryId);
        assertEquals(3, retrieved.size());

        // Cleanup
        memoryStore.deleteMessages(memoryId);
    }

    @Test
    void testMultipleMemoryIdsAreIsolated() {
        String memoryId1 = "test-isolation-1-" + System.currentTimeMillis();
        String memoryId2 = "test-isolation-2-" + System.currentTimeMillis();

        // Save different messages to different memory IDs
        memoryStore.updateMessages(memoryId1, List.of(
                UserMessage.from("Message for story 1")));

        memoryStore.updateMessages(memoryId2, List.of(
                UserMessage.from("Message for story 2"),
                AiMessage.from("Response for story 2")));

        // Verify isolation
        List<ChatMessage> messages1 = memoryStore.getMessages(memoryId1);
        List<ChatMessage> messages2 = memoryStore.getMessages(memoryId2);

        assertEquals(1, messages1.size());
        assertEquals(2, messages2.size());

        assertEquals("Message for story 1", ((UserMessage) messages1.get(0)).singleText());
        assertEquals("Message for story 2", ((UserMessage) messages2.get(0)).singleText());

        // Cleanup
        memoryStore.deleteMessages(memoryId1);
        memoryStore.deleteMessages(memoryId2);
    }

    private static boolean isNeo4jAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 7687), 300);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
