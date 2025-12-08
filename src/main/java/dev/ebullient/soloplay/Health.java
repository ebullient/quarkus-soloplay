package dev.ebullient.soloplay;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import dev.ebullient.soloplay.ollama.OllamaApiClient;

@ApplicationScoped
@Path("/health")
public class Health {

    @RestClient
    OllamaApiClient ollamaApiClient;

    @GET
    @Path("/ollama")
    public Response ollama() {
        try {
            return ollamaApiClient.listTags();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Ollama service is unavailable: " + e.getMessage())
                    .build();
        }
    }
}
