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
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.ebullient.soloplay.IngestService;
import dev.ebullient.soloplay.LoreAssistant;
import dev.ebullient.soloplay.MarkdownAugmenter;
import dev.ebullient.soloplay.health.Neo4jHealth;
import io.quarkus.logging.Log;

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

    @Inject
    Neo4jHealth neo4jHealth;

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
     * List all available adventures from ingested documents.
     * Adventures are identified by having "adventures" in the sourceFile path.
     * Returns a JSON map of setting name -> list of adventure names.
     */
    @GET
    @Path("/adventures")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> listAdventures() {
        return ingestService.listAdventures();
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

    /**
     * Upload and ingest documents into a setting.
     * Accepts multipart form data with settingName and one or more document files.
     */
    @POST
    @Path("/ingest")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ingestDocuments(
            @RestForm String settingName,
            @RestForm("documents") List<FileUpload> files) {

        // Validate required fields
        if (settingName == null || settingName.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Setting name is required"))
                    .build();
        }

        if (files == null || files.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "At least one document file is required"))
                    .build();
        }

        // Verify Neo4j connectivity before processing files
        try {
            neo4jHealth.neo4jIsAvailable();
        } catch (Exception e) {
            Log.errorf("Neo4j not available: %s", e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Neo4j is not available: " + e.getMessage()))
                    .build();
        }

        // Process files using IngestService
        IngestService.IngestResult result = ingestService.ingestDocuments(settingName, files);

        // Convert result to API response format
        List<Map<String, String>> errors = result.errors().stream()
                .map(err -> Map.of(
                        "file", err.fileName(),
                        "error", err.errorMessage()))
                .toList();

        // Return result
        if (result.allFailed()) {
            // All files failed
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "error", "All files failed to process",
                            "failures", errors))
                    .build();
        } else if (!errors.isEmpty()) {
            // Some files failed
            return Response.status(Response.Status.PARTIAL_CONTENT)
                    .entity(Map.of(
                            "settingName", settingName,
                            "processedFiles", result.processedFiles(),
                            "successCount", result.successCount(),
                            "failures", errors))
                    .build();
        } else {
            // All files succeeded
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "settingName", settingName,
                            "processedFiles", result.processedFiles(),
                            "successCount", result.successCount()))
                    .build();
        }
    }
}
