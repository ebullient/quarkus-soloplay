package dev.ebullient.soloplay;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class CampaignResourceTest {

    @Test
    void testChatEndpointExists() {
        // Test GET endpoint with query parameter
        given()
            .queryParam("question", "test")
            .when().get("/campaign/chat")
            .then()
            .statusCode(200)
            .body(notNullValue());
    }

    @Test
    void testChatPostEndpointExists() {
        // Test POST endpoint with body
        given()
            .contentType("text/plain")
            .body("test question")
            .when().post("/campaign/chat")
            .then()
            .statusCode(200)
            .body(notNullValue());
    }

    @Test
    void testLoreEndpointExists() {
        // Test lore endpoint with query parameter
        given()
            .queryParam("question", "test lore question")
            .when().get("/campaign/lore")
            .then()
            .statusCode(200)
            .body(notNullValue());
    }

    @Test
    void testLoadSettingEndpointRequiresMultipart() {
        // Test that load-setting endpoint requires multipart form data
        given()
            .when().post("/campaign/load-setting")
            .then()
            .statusCode(415); // Unsupported Media Type
    }
}
