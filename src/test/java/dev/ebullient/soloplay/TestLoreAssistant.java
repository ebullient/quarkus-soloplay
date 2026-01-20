package dev.ebullient.soloplay;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import dev.ebullient.soloplay.ai.LoreAssistant;

@Alternative
@Priority(1)
@ApplicationScoped
public class TestLoreAssistant implements LoreAssistant {

    @Override
    public String lore(String question) {
        return "Test lore response";
    }
}
