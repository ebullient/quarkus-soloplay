package dev.ebullient.soloplay;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestQuery;

@ApplicationScoped
@Path("/campaign")
public class CampaignResource {

    @Inject
    ChatService chatService;

    // @Inject
    // CampaignService campaignService;

    @Inject
    ResponseAugmenter prettify;

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

    // @GET
    // @Path("/ask")
    // public String ask(String question) {
    //     return campaignAssistant.chat(question);
    // }

    // @POST
    // @Path("/load-setting")
    // @Consumes(MediaType.MULTIPART_FORM_DATA)
    // public Response loadSetting(
    //         @RestForm String settingName,
    //         @RestForm("image") FileUpload file) {

    //     campaignService.loadSetting(settingName);
    //     return Response.ok().build();
    // }

}
