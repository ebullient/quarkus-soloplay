package dev.ebullient.soloplay.web;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@Path("/inspect")
public class Inspect extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance inspect(String gameId);
    }

    @GET
    @Path("/")
    public TemplateInstance index() {
        return Templates.inspect(null);
    }

    @GET
    @Path("/{gameId}")
    public TemplateInstance inspect(@RestPath String gameId) {
        return Templates.inspect(gameId);
    }
}
