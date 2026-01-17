package dev.ebullient.soloplay.play;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.StringUtils;
import dev.ebullient.soloplay.play.ActorCreationAssistant.ActorCreationResponse;
import dev.ebullient.soloplay.play.model.Actor;
import dev.ebullient.soloplay.play.model.Draft;
import dev.ebullient.soloplay.play.model.Draft.ActorCreation;
import dev.ebullient.soloplay.play.model.Draft.ActorDetails;
import dev.ebullient.soloplay.play.model.GameState;
import dev.ebullient.soloplay.play.model.GameState.GamePhase;
import dev.ebullient.soloplay.play.model.Patch.ActorCreationPatch;
import dev.langchain4j.exception.LangChain4jException;
import io.quarkus.logging.Log;

@ApplicationScoped
public class ActorCreationEngine {
    static final Draft.ActorCreation EMPTY_DRAFT = new Draft.ActorCreation(null, null, null, null, false);
    static final String DRAFT_KEY = "actor_creation";

    private final Map<String, ActorCreation> draftsByGameId = new ConcurrentHashMap<>();

    @Inject
    GameRepository gameRepository;

    @Inject
    ActorCreationAssistant assistant;

    @Inject
    ObjectMapper objectMapper;

    public GameResponse processRequest(GameState game, String playerInput, GameEventEmitter emitter) {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(emitter, "emitter");

        String gameId = game.getGameId();
        game.setGamePhase(GamePhase.CHARACTER_CREATION);

        ActorCreation currentDraft = draftsByGameId.computeIfAbsent(gameId, k -> EMPTY_DRAFT);

        String trimmed = playerInput == null ? "" : playerInput.trim();
        if (GameEngine.isHelpCommand(trimmed)) {
            return help(game);
        }
        if (trimmed.equalsIgnoreCase("/reset")) {
            draftsByGameId.remove(gameId);
            return GameResponse.reply("Ok — cleared your character draft.", new GameEffect.DraftUpdate(DRAFT_KEY, null));
        }
        if (trimmed.equalsIgnoreCase("/draft")) {
            return GameResponse.reply(renderDraft(currentDraft));
        }
        if (trimmed.equalsIgnoreCase("/confirm")) {
            emitter.assistantDelta("Confirming character…\n");
            ActorCreation confirmed = new ActorCreation(
                    currentDraft.name(), currentDraft.actorClass(), currentDraft.level(),
                    currentDraft.details(), true);

            String missing = missingRequired(confirmed);
            if (missing != null) {
                return GameResponse.error("Can't confirm yet: " + missing);
            }

            Actor actor = new Actor(gameId, confirmed);
            emitter.assistantDelta("Saving character…\n");
            gameRepository.saveActor(actor);

            game.setGamePhase(game.getGamePhase().next());

            draftsByGameId.remove(gameId);
            return GameResponse.reply("Created your character: **" + actor.getName() + "** (" + actor.getActorClass() + " "
                    + actor.getLevel() + ").");
        }

        emitter.assistantDelta("Digging through the details…\n");

        ActorCreationResponse response = null;
        try {
            response = handleAssistantResponse(game, currentDraft, trimmed); // may throw
        } catch (ActorResponseException actorEx) {
            emitter.assistantDelta("Hmmm. That didn't go as planned. Retrying…\n");
            response = handleAssistantResponse(game, currentDraft, trimmed); // may throw
        }

        // All is well with parsed response
        var message = response.messageMarkdown();
        var patch = response.patch();

        emitter.assistantDelta("Updating your character…\n");
        ActorCreation updatedDraft = applyPatch(currentDraft, patch);
        draftsByGameId.put(gameId, updatedDraft);

        return GameResponse.reply(
                (message == null ? "ok." : message) + "\n\n" + renderDraft(updatedDraft)
                        + "\n\nUse `/confirm` if this looks good to you.",
                new GameEffect.DraftUpdate(DRAFT_KEY, updatedDraft));
    }

    private ActorCreationResponse handleAssistantResponse(GameState state, Draft.ActorCreation currentDraft,
            String playerInput) {
        ActorCreationResponse response = null;
        try {
            if ((playerInput.isBlank() || playerInput.equals("/start")) && currentDraft == EMPTY_DRAFT) {
                response = assistant.start(state.getGameId(), state.getAdventureName());
            } else {
                response = assistant.turn(state.getGameId(), state.getAdventureName(), currentDraft, playerInput);
            }
        } catch (LangChain4jException ex) {
            throw ex;
        } catch (Exception e) {
            Log.errorf(e, "Exception reading turn response: %s", debugReturnValue(response));
            throw new ActorResponseException("bad response format", e);
        }
        if (response == null || response.messageMarkdown() == null) {
            Log.errorf("Markdown text response was missing", debugReturnValue(response));
            throw new ActorResponseException("Markdown response was missing");
        }
        return response;
    }

    public GameResponse help(GameState game) {
        return GameResponse.reply("""
                Character creation commands:

                - `/draft`: show your current draft
                - `/reset`: clear the current draft
                - `/confirm`: create the character (requires name, class, level)
                - `/help` (or `help`, `?`): show commands
                """);
    }

    String debugReturnValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            // no-op. Best effort
        }
        return value == null ? "null" : "unable to serialize";
    }

    static String missingRequired(ActorCreation draft) {
        if (draft == null) {
            return "no draft";
        }
        if (draft.name() == null || draft.name().isBlank()) {
            return "missing name";
        }
        if (draft.actorClass() == null || draft.actorClass().isBlank()) {
            return "missing class";
        }
        if (draft.level() == null || draft.level() < 1) {
            return "missing/invalid level";
        }
        return null;
    }

    static ActorCreation applyPatch(ActorCreation current, ActorCreationPatch patch) {
        if (patch == null) {
            return current;
        }
        ActorDetails mergedDetails = mergeDetails(current.details(), patch.details());
        return new Draft.ActorCreation(
                StringUtils.firstNonBlank(patch.name(), current.name()),
                StringUtils.firstNonBlank(patch.actorClass(), current.actorClass()),
                patch.level() != null ? patch.level() : current.level(),
                mergedDetails,
                current.confirmed());
    }

    static ActorDetails mergeDetails(ActorDetails current, ActorDetails patch) {
        if (current == null) {
            return patch;
        }
        if (patch == null) {
            return current;
        }
        return new Draft.ActorDetails(
                StringUtils.firstNonBlank(patch.summary(), current.summary()),
                StringUtils.firstNonBlank(patch.description(), current.description()),
                patch.tags() != null ? patch.tags() : current.tags(),
                patch.aliases() != null ? patch.aliases() : current.aliases());
    }

    static String renderDraft(ActorCreation draft) {
        if (draft == null) {
            return "No current draft.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Current character draft:\n\n");
        sb.append("- **Name**: ").append(StringUtils.valueOrPlaceholder(draft.name())).append("\n");
        sb.append("- **Class**: ").append(StringUtils.valueOrPlaceholder(draft.actorClass())).append("\n");
        sb.append("- **Level**: ").append(StringUtils.valueOrPlaceholder(draft.level())).append("\n");
        if (draft.details() != null) {
            sb.append("- **Summary**: ").append(StringUtils.valueOrPlaceholder(draft.details().summary())).append("\n");
            sb.append("- **Description**: ").append(StringUtils.valueOrPlaceholder(draft.details().description())).append("\n");
            sb.append("- **Aliases**: ").append(StringUtils.valueOrPlaceholder(draft.details().aliases())).append("\n");
            sb.append("- **Tags**: ").append(StringUtils.valueOrPlaceholder(draft.details().tags())).append("\n");
        }
        return sb.toString();
    }
}
