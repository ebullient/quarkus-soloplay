package dev.ebullient.soloplay;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;

import dev.ebullient.soloplay.health.Neo4jHealth;

@ApplicationScoped
@Path("/campaign")
public class CampaignResource {

    @Inject
    ChatAssistant chatService;

    @Inject
    SettingAssistant settingAssistant;

    @Inject
    IngestService campaignService;

    @Inject
    MarkdownAugmenter prettify;

    @Inject
    Neo4jHealth neo4jHealth;

    @Inject
    StoryRepository storyRepository;

    // 1. Set a basic chat interface though to Ollama

    @GET
    @Path("/chat")
    @Produces(MediaType.TEXT_HTML)
    public String chat(@RestQuery String question) {
        String response = chatService.chat(question);
        return prettify.markdownToHtml(response);
    }

    @POST
    @Path("/chat")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    public String postChat(String question) {
        String response = chatService.chat(question);
        return prettify.markdownToHtml(response);
    }

    // 2. Setting
    //
    // Set up embeddings for campaign-specific knowledge
    // Note: Document upload is now handled by the Renarde controller at /load-setting
    // for proper form handling with CSRF protection and flash messages
    //
    // Ask a lore question

    @GET
    @Path("/lore")
    @Produces(MediaType.TEXT_HTML)
    public String lore(@RestQuery String question) {
        String response = settingAssistant.lore(question);
        return prettify.markdownToHtml(response);
    }

    @POST
    @Path("/lore")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    public String postLore(String question) {
        String response = settingAssistant.lore(question);
        return prettify.markdownToHtml(response);
    }

    // 3. Story-Specific Data (Tools, etc.)
    // More interactions available through Renarde controllers
    //
    // Get list of known story thread IDs

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getStoryThreadIds() {
        return storyRepository.getStoryThreadIds();
    }

}
