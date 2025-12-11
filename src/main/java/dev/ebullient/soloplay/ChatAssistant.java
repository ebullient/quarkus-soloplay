package dev.ebullient.soloplay;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * This interface is annotated with @RegisterAiService to enable automatic
 * registration and integration with AI services.
 * The chat method takes a user message as input and returns a generated response.
 */
@RegisterAiService
@ApplicationScoped
public interface ChatAssistant {

    /**
     * Generates a chat response based on the user's message.
     *
     * @param userMessage
     * @return
     */
    @UserMessage("Input: {userMessage}")
    String chat(String userMessage);

}