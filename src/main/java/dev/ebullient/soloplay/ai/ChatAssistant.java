package dev.ebullient.soloplay.ai;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@SystemMessage("""
        You are a helpful AI assistant.

        Be conversational and friendly. Provide clear, concise answers.
        When uncertain, say so rather than guessing.
        """)
@RegisterAiService(chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class)
@ApplicationScoped
public interface ChatAssistant {

    String chat(@UserMessage String userMessage);
}
