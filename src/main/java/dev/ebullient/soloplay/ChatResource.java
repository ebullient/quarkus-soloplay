package dev.ebullient.soloplay;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;

/**
 * Generic LLM chat interface - independent of any specific setting or story.
 * Provides direct access to the underlying chat model.
 */
@ApplicationScoped
@Path("/api/chat")
public class ChatResource {

    @Inject
    ChatAssistant chatService;

    @Inject
    MarkdownAugmenter prettify;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String chat(@RestQuery String question) {
        String response = chatService.chat(question);
        return prettify.markdownToHtml(response);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    public String postChat(String question) {
        String response = chatService.chat(question);
        return prettify.markdownToHtml(response);
    }
}
