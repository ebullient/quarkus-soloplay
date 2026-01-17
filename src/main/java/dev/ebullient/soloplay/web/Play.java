package dev.ebullient.soloplay.web;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

public class Play extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance play(String gameId);
    }

    @GET
    @Path("/play")
    public TemplateInstance play() {
        // redirect to game
        return redirect(Game.class).index();
    }

    /**
     * Serve the main play page (WebSocket-based streaming chat).
     */
    @GET
    @Path("/play/{gameId}")
    public TemplateInstance play(@RestPath String gameId) {
        return Templates.play(gameId);
    }
}
