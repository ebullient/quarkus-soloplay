package dev.ebullient.soloplay.web;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.play.model.PlayerActor;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@Path("/party")
public class Party extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance party(String gameId, List<PlayerActor> members);
    }

    @Inject
    GameRepository gameRepository;

    @GET
    @Path("/")
    public TemplateInstance index() {
        return Templates.party(null, List.of());
    }

    @GET
    @Path("/{gameId}")
    public TemplateInstance party(@RestPath String gameId) {
        List<PlayerActor> members = gameRepository.listPlayerActors(gameId);
        return Templates.party(gameId, members);
    }

    @POST
    @Path("/{gameId}/{actorId}")
    public void update(
            @RestPath String gameId,
            @RestPath String actorId,
            @RestForm String name,
            @RestForm String summary,
            @RestForm String description,
            @RestForm String actorClass,
            @RestForm Integer level,
            @RestForm String aliases,
            @RestForm String tags) {

        PlayerActor actor = gameRepository.findPlayerActorByNameOrAlias(gameId, actorId);
        if (actor == null) {
            flash("error", "Player actor not found: " + actorId);
            party(gameId);
            return;
        }

        // Apply updates
        if (name != null && !name.isBlank()) {
            actor.setName(name.trim());
        }
        if (summary != null) {
            actor.setSummary(summary.isBlank() ? null : summary.trim());
        }
        if (description != null) {
            actor.setDescription(description.isBlank() ? null : description.trim());
        }
        if (actorClass != null) {
            actor.setActorClass(actorClass.isBlank() ? null : actorClass.trim());
        }
        if (level != null) {
            actor.setLevel(level);
        }
        if (aliases != null) {
            List<String> aliasList = aliases.isBlank()
                    ? List.of()
                    : Arrays.stream(aliases.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
            actor.setAliases(aliasList);
        }
        if (tags != null) {
            List<String> tagList = tags.isBlank()
                    ? List.of()
                    : Arrays.stream(tags.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
            actor.setTags(tagList);
        }

        gameRepository.saveActor(actor);
        gameRepository.refreshTheParty(gameId);

        flash("success", "Updated " + actor.getName());
        redirect(Party.class).party(gameId);
    }
}
