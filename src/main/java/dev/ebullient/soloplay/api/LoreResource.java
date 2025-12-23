package dev.ebullient.soloplay.api;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;

import dev.ebullient.soloplay.IngestService;
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

    @Inject
    IngestService ingestService;

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

    /**
     * List all ingested files grouped by setting.
     * Returns a JSON map of setting name -> list of files with embedding counts.
     */
    @GET
    @Path("/files")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<Map<String, Object>>> listFiles() {
        return ingestService.listIngestedFiles();
    }

    /**
     * Delete a specific file from a setting.
     * Query params: settingName and sourceFile
     */
    @DELETE
    @Path("/files")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> deleteFile(
            @RestQuery String settingName,
            @RestQuery String sourceFile) {
        int deleteCount = ingestService.deleteFile(settingName, sourceFile);
        return Map.of(
                "deleted", deleteCount,
                "settingName", settingName,
                "sourceFile", sourceFile);
    }

    /**
     * Delete all files from a setting.
     * Query param: settingName
     */
    @DELETE
    @Path("/setting")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> deleteSetting(@RestQuery String settingName) {
        int deleteCount = ingestService.deleteSetting(settingName);
        return Map.of(
                "deleted", deleteCount,
                "settingName", settingName);
    }
}
