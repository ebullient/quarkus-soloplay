package dev.ebullient.soloplay.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.OutputGuardrails;
import io.quarkiverse.langchain4j.RegisterAiService;

@SystemMessage("""
        You are a helpful AI assistant.

        Be conversational and friendly. Provide clear, concise answers.
        When uncertain, say so rather than guessing.

        RESPONSE FORMAT: Return a JSON object with this structure:
        {
            "response": "Your complete answer here"
        }
        """)
@RegisterAiService(chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class)
@OutputGuardrails(JsonChatResponseGuardrail.class)
public interface ChatAssistant {

    JsonChatResponse chat(@UserMessage String userMessage);
}
