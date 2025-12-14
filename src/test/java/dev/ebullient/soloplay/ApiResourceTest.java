package dev.ebullient.soloplay;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ApiResourceTest {

    @Test
    void testChatEndpointExists() {
        // Test GET endpoint with query parameter
        given()
                .queryParam("question", "test")
                .when().get("/api/chat")
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
                .when().post("/api/chat")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    void testLoreEndpointExists() {
        // Test lore endpoint with query parameter
        given()
                .queryParam("question", "test lore question")
                .when().get("/api/lore")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    void testLorePostEndpointExists() {
        // Test lore endpoint with query parameter
        given()
                .contentType("text/plain")
                .body("test question")
                .when().post("/api/lore")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    // Note: load-setting functionality moved to Renarde controller at /load-setting
    // for proper web form handling with CSRF protection and flash messages
}
