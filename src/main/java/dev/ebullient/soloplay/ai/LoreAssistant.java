package dev.ebullient.soloplay.ai;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.RegisterAiService;

@ApplicationScoped
@RegisterAiService(retrievalAugmentor = LoreRetriever.class)
public interface LoreAssistant {

    String lore(String question);

}
