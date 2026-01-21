package dev.ebullient.soloplay.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.LoreRepository;
import dev.langchain4j.agent.tool.Tool;

/**
 * AI Tools for lore document retrieval.
 * Provides cross-reference resolution for campaign documents.
 */
@ApplicationScoped
public class LoreTools {

    @Inject
    LoreRepository loreRepository;

    @Tool("""
            Retrieve lore document content by exact filename.
            Use to resolve cross-references in campaign documents.

            Example: getLoreDocument("feats/magic-initiate-xphb.md")
            Returns the full document text, or an error message if not found.
            """)
    public String getLoreDocument(String filename) {
        String content = loreRepository.getDocumentByFilename(filename);
        if (content == null) {
            return "Document not found: " + filename;
        }
        return content;
    }
}
