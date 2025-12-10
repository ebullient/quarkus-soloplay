package dev.ebullient.soloplay;

import java.nio.file.Files;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@ApplicationScoped
@Path("/campaign")
public class CampaignResource {

    @Inject
    ChatAssistant chatService;

    @Inject
    SettingAssistant settingAssistant;

    @Inject
    CampaignService campaignService;

    @Inject
    ResponseAugmenter prettify;

    @Inject
    Neo4jHealth neo4jHealth;

    // 1. Set a basic chat interface though to Ollama

    @GET
    @Path("/chat")
    public String chat(@RestQuery String question) {
        String response = chatService.chat(question);
        return prettify.markdownToHtml(response);
    }

    @POST
    @Path("/chat")
    public String postChat(String question) {
        String response = chatService.chat(question);
        return prettify.markdownToHtml(response);
    }

    // 2. Set up embeddings for campaign-specific knowledge

    @POST
    @Path("/load-setting")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response loadSetting(
            @RestForm String settingName,
            @RestForm("documents") List<FileUpload> files) {

        // Verify Neo4j connectivity before processing files
        try {
            neo4jHealth.neo4jIsAvailable();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Neo4j is not available: " + e.getMessage())
                    .build();
        }

        // Process multiple files
        for (var file : files) {
            try {
                String content = Files.readString(file.uploadedFile());
                campaignService.loadSetting(settingName, file.fileName(), content);
            } catch (Exception e) {
                e.printStackTrace();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing file: " + file.fileName())
                        .build();
            }
        }
        return Response.ok("Setting '" + settingName + "' updated with " + files.size() + " files.").build();
    }

    // 3. Ask a lore question

    @GET
    @Path("/lore")
    public String lore(@RestQuery String question) {
        String response = settingAssistant.lore(question);
        return prettify.markdownToHtml(response);
    }

    @POST
    @Path("/lore")
    public String postLore(String question) {
        String response = settingAssistant.lore(question);
        return prettify.markdownToHtml(response);
    }


}
