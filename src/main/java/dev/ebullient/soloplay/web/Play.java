package dev.ebullient.soloplay.web;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

public class Play extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance play();
    }

    /**
     * Serve the main play page (WebSocket-based streaming chat).
     */
    @GET
    @Path("/play")
    public TemplateInstance play() {
        return Templates.play();
    }
}
