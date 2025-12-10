package dev.ebullient.soloplay;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.RegisterAiService;

@ApplicationScoped
@RegisterAiService(retrievalAugmentor = SettingRetriever.class)
public interface SettingAssistant {

    String lore(String question);

}
