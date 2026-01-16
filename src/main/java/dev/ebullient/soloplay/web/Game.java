package dev.ebullient.soloplay.web;

import static dev.ebullient.soloplay.StringUtils.slugify;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.LoreRepository;
import dev.ebullient.soloplay.play.model.GameState;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@Path("/game")
public class Game extends Controller {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<GameState> games);

        public static native TemplateInstance create(List<String> adventures);
    }

    @Inject
    GameRepository gameRepository;

    @Inject
    LoreRepository loreRepository;

    /**
     * Landing page - show all games with option to create new.
     */
    @GET
    @Path("/")
    public TemplateInstance index() {
        return Templates.index(gameRepository.listGames());
    }

    @GET
    @Path("/create")
    public TemplateInstance create() {
        List<String> adventures = loreRepository.listAdventures();
        return Templates.create(adventures);
    }

    /**
     * Handle game creation form submission.
     * Redirects to the play page on success to ensure URL matches the page.
     */
    @POST
    @Path("/create")
    public void createPost(
            @RestForm String name,
            @RestForm String adventureName) {

        if (name == null || name.isBlank()) {
            flash("error", "Please provide a name for your game");
            create();
            return;
        }

        GameState game;
        try {
            game = gameRepository.createGame(slugify(name), adventureName);
        } catch (IllegalArgumentException e) {
            flash("error", e.getMessage());
            create();
            return;
        }

        flash("success", "Game created: " + name);
        redirect(Play.class).play(game.getGameId());
    }
}
