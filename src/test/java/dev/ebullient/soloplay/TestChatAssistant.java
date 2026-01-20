package dev.ebullient.soloplay;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import dev.ebullient.soloplay.ai.ChatAssistant;

@Alternative
@Priority(1)
@ApplicationScoped
public class TestChatAssistant implements ChatAssistant {

    @Override
    public String chat(String userMessage) {
        return "Test response";
    }
}
