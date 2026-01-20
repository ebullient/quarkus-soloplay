package dev.ebullient.soloplay.ai;

import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.logging.Log;

@ApplicationScoped
public class LoreRetriever implements Supplier<RetrievalAugmentor> {
    private final RetrievalAugmentor augmentor;

    /**
     * Custom prompt template that clearly frames RAG content as reference material.
     * This helps prevent the LLM from echoing the content instead of using it.
     */
    private static final PromptTemplate RAG_PROMPT_TEMPLATE = PromptTemplate.from("""
            {{userMessage}}

            === REFERENCE MATERIAL (use as context, do NOT echo or summarize) ===
            The following is background information to inform your response.
            DO NOT repeat or summarize this content verbatim. Use it to ground your response.

            {{contents}}

            === END REFERENCE MATERIAL ===
            """);

    public LoreRetriever(
            EmbeddingStore<TextSegment> store,
            EmbeddingModel model,
            @ConfigProperty(name = "campaign.setting.maxResults", defaultValue = "5") int maxResults) {
        EmbeddingStoreContentRetriever baseRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .maxResults(maxResults)
                .minScore(0.3) // Filter out low-quality matches
                .build();

        // Wrap with logging
        var loggingRetriever = new dev.langchain4j.rag.content.retriever.ContentRetriever() {
            @Override
            public List<Content> retrieve(Query query) {
                Log.debugf("RAG Query: %s", query.text());
                List<Content> results = baseRetriever.retrieve(query);
                Log.debugf("RAG Retrieved %d results", results.size());
                for (int i = 0; i < results.size(); i++) {
                    Content c = results.get(i);
                    Log.debugf("  [%d] %s...", i,
                            c.textSegment().text().substring(0, Math.min(100, c.textSegment().text().length())));
                }
                return results;
            }
        };

        // Custom content injector that frames RAG content clearly
        var contentInjector = DefaultContentInjector.builder()
                .promptTemplate(RAG_PROMPT_TEMPLATE)
                .metadataKeysToInclude(List.of("name", "source", "filename"))
                .build();

        augmentor = DefaultRetrievalAugmentor
                .builder()
                .contentRetriever(loggingRetriever)
                .contentInjector(contentInjector)
                .build();
    }

    @Override
    public RetrievalAugmentor get() {
        return augmentor;
    }
}
