package dev.ebullient.soloplay.ollama;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * OllamaApiClient is a REST client interface for interacting with the Ollama API.
 * It is annotated with @RegisterRestClient to enable automatic registration
 * and configuration using the specified config key "ollama-api".
 */
@ApplicationScoped
@RegisterRestClient(configKey = "ollama-api")
public interface OllamaApiClient {

    /**
     * Lists the tags available from the Ollama API.
     * @return
     */
    @GET
    @Path("/api/tags")
    Response listTags();

    /**
     * Lists the tags available from the Ollama API.
     * @return
     */
    @POST
    @Path("/api/embeddings")
    OllamaEmbeddingResponse generateEmbedding(OllamaEmbeddingRequest request);

    record OllamaEmbeddingRequest(String model, String prompt) {}
    record OllamaEmbeddingResponse(List<Double> embedding) {}
}