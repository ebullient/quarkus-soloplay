package dev.ebullient.soloplay.web;

import jakarta.ws.rs.Path;

import io.quarkiverse.renarde.Controller;

/**
 * Renarde controller for story pages and operations.
 * Serves chat, lore, and document ingestion pages with CSRF protection.
 */
@Path("/")
public class Index extends Controller {

}
