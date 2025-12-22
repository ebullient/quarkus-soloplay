package dev.ebullient.soloplay.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;

import dev.ebullient.soloplay.LoreAssistant;
import dev.ebullient.soloplay.MarkdownAugmenter;

/**
 * RAG-based lore query interface.
 * Queries against ingested campaign documents with semantic search.
 * Document ingestion is handled via the web UI at /ingest.
 */
@ApplicationScoped
@Path("/api/lore")
public class LoreResource {

    @Inject
    LoreAssistant settingAssistant;

    @Inject
    MarkdownAugmenter prettify;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String lore(@RestQuery String question) {
        String response = settingAssistant.lore(question);
        return prettify.markdownToHtml(response);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    public String postLore(String question) {
        String response = settingAssistant.lore(question);
        return prettify.markdownToHtml(response);
    }
}
