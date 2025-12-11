package dev.ebullient.soloplay.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import dev.ebullient.soloplay.ollama.OllamaApiClient;
import dev.ebullient.soloplay.ollama.OllamaApiClient.OllamaEmbeddingRequest;

@ApplicationScoped
@Path("/ollama")
public class OllamaHealth {

    @RestClient
    OllamaApiClient ollamaApiClient;

    @ConfigProperty(name = "quarkus.langchain4j.ollama.embedding-model.model-name")
    String embeddingModelName;

    @GET
    @Path("/tags")
    public Response ollamaTags() {
        try {
            return ollamaApiClient.listTags();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Ollama service is unavailable: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/embedding")
    public Response ollamaEmbeddings() {
        try {
            var response = ollamaApiClient.generateEmbedding(
                    new OllamaEmbeddingRequest(embeddingModelName, "Test embedding"));

            return Response.ok()
                    .entity("Embedding dimension: " + response.embedding().size())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Ollama service is unavailable: " + e.getMessage())
                    .build();
        }
    }
}
