package dev.ebullient.soloplay.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.neo4j.driver.Driver;

@ApplicationScoped
@Path("/neo4j")
public class Neo4jHealth {

    @Inject
    Driver neo4jDriver;

    public boolean neo4jIsAvailable() throws Exception {
        neo4jDriver.verifyConnectivity();
        return true;
    }

    @GET
    @Path("/status")
    public Response neo4jStatus() {
        try {
            // Verify connectivity with a simple query
            neo4jDriver.verifyConnectivity();

            return Response.ok()
                    .entity("Neo4j connection: OK")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Neo4j service is unavailable: " + e.getMessage())
                    .build();
        }
    }
}
