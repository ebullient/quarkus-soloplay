package dev.ebullient.soloplay.web;

import java.nio.file.Files;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.ebullient.soloplay.IngestService;
import dev.ebullient.soloplay.health.Neo4jHealth;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

/**
 * Renarde controller for campaign pages and operations.
 * Serves chat, lore, and document ingestion pages with CSRF protection.
 */
@Path("/")
public class Campaign extends Controller {
    private static final Logger LOG = Logger.getLogger(Campaign.class);

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance chat();

        public static native TemplateInstance lore();

        public static native TemplateInstance ingest();
    }

    @Inject
    IngestService campaignService;

    @Inject
    Neo4jHealth neo4jHealth;

    /**
     * Serve the main chat page.
     */
    @GET
    @Path("/chat")
    public TemplateInstance index() {
        return Templates.chat();
    }

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
        return Templates.ingest();
    }

    /**
     * Handle document upload form submission.
     * Ingests campaign documents into the knowledge base with proper validation and error handling.
     */
    @POST
    @Path("/load-setting")
    public void loadSetting(
            @RestForm String settingName,
            @RestForm("documents") List<FileUpload> files) {

        if (settingName == null || settingName.isBlank()) {
            flash("error", "Please provide a setting name");
            return;
        }

        if (files == null || files.isEmpty()) {
            flash("error", "Please select at least one file to upload");
            return;
        }

        // Verify Neo4j connectivity before processing files
        try {
            neo4jHealth.neo4jIsAvailable();
        } catch (Exception e) {
            LOG.error("Neo4j not available", e);
            flash("error", "Neo4j is not available: " + e.getMessage());
            return;
        }

        // Process multiple files
        int successCount = 0;
        for (var file : files) {
            try {
                String content = Files.readString(file.uploadedFile());
                campaignService.loadSetting(settingName, file.fileName(), content);
                successCount++;
            } catch (Exception e) {
                LOG.error("Error processing file: " + file.fileName(), e);
                flash("error", "Error processing file " + file.fileName() + ": " + e.getMessage());
                return;
            }
        }

        flash("success", "Setting '" + settingName + "' updated with " + successCount + " file(s)");
        // Renarde automatically redirects back to the referring page
    }
}
