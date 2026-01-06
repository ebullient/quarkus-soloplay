package dev.ebullient.soloplay.web;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.ebullient.soloplay.IngestService;
import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.health.Neo4jHealth;
import io.quarkiverse.renarde.Controller;
import io.quarkus.logging.Log;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

public class Lore extends Controller {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance lore();

        public static native TemplateInstance ingest(List<String> availableSettings);
    }

    @Inject
    IngestService campaignService;

    @Inject
    Neo4jHealth neo4jHealth;

    @Inject
    StoryRepository storyRepository;

    /**
     * Serve the lore query page.
     */
    @GET
    @Path("/lore")
    public TemplateInstance lore() {
        return Templates.lore();
    }

    /**
     * Serve the document ingestion page.
     */
    @GET
    @Path("/ingest")
    public TemplateInstance ingest() {
        List<String> availableSettings = storyRepository.getAvailableSettings();
        return Templates.ingest(availableSettings);
    }

    /**
     * Handle document upload form submission.
     * Ingests campaign documents into the knowledge base with proper validation and
     * error handling.
     */
    @POST
    @Path("/load-setting")
    public TemplateInstance loadSetting(
            @RestForm String settingName,
            @RestForm("documents") List<FileUpload> files) {

        if (settingName == null || settingName.isBlank()) {
            flash("error", "Please provide a setting name");
            return ingest();
        }

        if (files == null || files.isEmpty()) {
            flash("error", "Please select at least one file to upload");
            return ingest();
        }

        // Verify Neo4j connectivity before processing files
        try {
            neo4jHealth.neo4jIsAvailable();
        } catch (Exception e) {
            Log.error("Neo4j not available", e);
            flash("error", "Neo4j is not available: " + e.getMessage());
            return ingest();
        }

        // Process files using IngestService
        IngestService.IngestResult result = campaignService.ingestDocuments(settingName, files);

        // Report all errors and successes
        if (result.allFailed()) {
            // All files failed - show all errors
            StringBuilder errorMsg = new StringBuilder("All files failed to process:\n");
            for (IngestService.FileError error : result.errors()) {
                errorMsg.append("- ").append(error.fileName()).append(": ").append(error.errorMessage()).append("\n");
            }
            flash("error", errorMsg.toString());
            return ingest();
        } else if (result.hasErrors()) {
            // Partial success - show what worked and what failed
            StringBuilder msg = new StringBuilder();
            msg.append("Processed ").append(result.successCount()).append(" file(s) successfully");
            if (!result.processedFiles().isEmpty()) {
                msg.append(" (").append(String.join(", ", result.processedFiles())).append(")");
            }
            msg.append(". Failed files:\n");
            for (IngestService.FileError error : result.errors()) {
                msg.append("- ").append(error.fileName()).append(": ").append(error.errorMessage()).append("\n");
            }
            flash("warning", msg.toString());
            return ingest();
        }

        flash("success", "Setting '" + settingName + "' updated with " + result.successCount() + " file(s)");
        return ingest();
    }
}
