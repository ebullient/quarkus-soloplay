package dev.ebullient.soloplay.play;

import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.StringUtils;
import dev.ebullient.soloplay.play.ActorCreationAssistant.ActorCreationResponse;
import dev.ebullient.soloplay.play.model.Draft;
import dev.ebullient.soloplay.play.model.Draft.Details;
import dev.ebullient.soloplay.play.model.Draft.PlayerActorDraft;
import dev.ebullient.soloplay.play.model.GameState;
import dev.ebullient.soloplay.play.model.GameState.GamePhase;
import dev.ebullient.soloplay.play.model.Patch.PlayerActorCreationPatch;
import dev.ebullient.soloplay.play.model.PlayerActor;
import dev.langchain4j.exception.LangChain4jException;
import io.quarkus.logging.Log;

@ApplicationScoped
public class ActorCreationEngine {
    static final PlayerActorDraft EMPTY_DRAFT = new PlayerActorDraft(null, null, null, null, false);
    static final String DRAFT_KEY = "actor_creation";

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

        var currentDraft = getCurrentDraft(game);

        String trimmed = playerInput == null ? "" : playerInput.trim();
        if (GameEngine.isHelpCommand(trimmed)) {
            return help(game);
        }
        if ("/cancel".equalsIgnoreCase(trimmed) || "/reset".equalsIgnoreCase(trimmed)) {
            cleanupDraft(game);
            if ("/cancel".equalsIgnoreCase(trimmed)) {
                String partyMembers = gameRepository.listPlayerActors(gameId).stream()
                        .map(pa -> "%s, %s, level %s".formatted(pa.getName(), pa.getActorClass(), pa.getLevel()))
                        .collect(Collectors.joining("; "));
                if (!partyMembers.isBlank()) {
                    game.setGamePhase(game.getGamePhase().next());

                    return GameResponse.reply("""
                            Exiting character creation.

                            Current party: %s

                            Use `/newcharacter` to create an additional character, or `/start` to start or resume your game.
                            """.stripIndent().formatted(partyMembers));
                }
            }
            return GameResponse.reply("Ok — cleared your character draft.",
                    new GameEffect.DraftUpdate(DRAFT_KEY, null));
        }
        if ("/draft".equalsIgnoreCase(trimmed)) {
            return GameResponse.reply(renderDraft(currentDraft));
        }
        if ("/confirm".equalsIgnoreCase(trimmed)) {
            emitter.assistantDelta("Confirming character…\n");
            var confirmed = new PlayerActorDraft(
                    currentDraft.name(),
                    currentDraft.details(),
                    currentDraft.actorClass(), currentDraft.level(),
                    true);

            String missing = missingRequired(confirmed);
            if (missing != null) {
                return GameResponse.error("Can't confirm yet: " + missing);
            }

            PlayerActor actor = new PlayerActor(gameId, confirmed);
            emitter.assistantDelta("Saving character…\n");
            gameRepository.saveActor(actor);
            cleanupDraft(game);

            game.setGamePhase(game.getGamePhase().next());
            return GameResponse.reply("""
                    Created your character: **%s** (%s, level %s).

                    Use `/newcharacter` to create an additional character, or `/start` to start or resume your game.
                    """.stripIndent().formatted(actor.getName(), actor.getActorClass(), actor.getLevel()));
        }

        emitter.assistantDelta("The GM is thinking…\n");

        ActorCreationResponse response = null;
        try {
            response = handleAssistantResponse(game, currentDraft, trimmed); // may throw
        } catch (AssistantResponseException actorEx) {
            emitter.assistantDelta("Hmmm. That didn't go as planned. Retrying…\n");
            response = handleAssistantResponse(game, currentDraft, trimmed); // may throw
        }

        // All is well with parsed response
        var message = response.messageMarkdown();
        var patch = response.patch();

        emitter.assistantDelta("Updating your character…\n");
        PlayerActorDraft updatedDraft = applyPatch(currentDraft, patch);
        updateDraft(game, updatedDraft);

        return GameResponse.reply(
                (message == null ? "ok." : message) + "\n\n" + renderDraft(updatedDraft)
                        + "\n\nUse `/confirm` if this looks good to you.",
                new GameEffect.DraftUpdate(DRAFT_KEY, updatedDraft));
    }

    private ActorCreationResponse handleAssistantResponse(GameState state, PlayerActorDraft currentDraft,
            String playerInput) {
        String chatMemoryId = state.getGameId() + "-character";
        String rawResponse;

        try {
            if ((playerInput.isBlank() || playerInput.equals("/start")) && currentDraft == EMPTY_DRAFT) {
                rawResponse = assistant.start(chatMemoryId, state.getGameId(), state.getAdventureName());
            } else {
                rawResponse = assistant.turn(chatMemoryId, state.getGameId(), state.getAdventureName(), currentDraft,
                        playerInput);
            }
        } catch (LangChain4jException ex) {
            throw ex;
        }

        return parseResponse(rawResponse);
    }

    ActorCreationResponse parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            Log.error("Empty response from assistant");
            throw new AssistantResponseException("Empty response from assistant", true);
        }

        try {
            ActorCreationResponse response = objectMapper.readValue(rawResponse, ActorCreationResponse.class);
            if (response.messageMarkdown() == null) {
                Log.errorf("Markdown text response was missing. Raw: %s", rawResponse);
                throw new AssistantResponseException("Markdown response was missing", true);
            }
            return response;
        } catch (JsonParseException | JsonMappingException e) {
            Log.errorf(e, "Malformed JSON from assistant. Raw: %s", rawResponse);
            throw new AssistantResponseException("Malformed JSON: " + e.getOriginalMessage(), e, true);
        } catch (Exception e) {
            Log.errorf(e, "Failed to parse response. Raw: %s", rawResponse);
            throw new AssistantResponseException("Unable to parse response: " + e.getMessage(), e, false);
        }
    }

    public GameResponse help(GameState game) {
        return GameResponse.reply("""
                Character creation commands:

                - `/draft`: show your current draft
                - `/cancel`: exit character creation (as long as at least one party member exists)
                - `/confirm`: create the character (requires name, class, level)
                - `/reset`: clear the current draft
                - `/help` (or `help`, `?`): show commands
                """);
    }

    PlayerActorDraft getCurrentDraft(GameState game) {
        return game.getDraftOrDefault(DRAFT_KEY, PlayerActorDraft.class, EMPTY_DRAFT);
    }

    void cleanupDraft(GameState game) {
        game.removeDraft(DRAFT_KEY);
    }

    void updateDraft(GameState game, PlayerActorDraft updatedDraft) {
        game.putDraft(DRAFT_KEY, updatedDraft);
    }

    static String missingRequired(PlayerActorDraft draft) {
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

    static PlayerActorDraft applyPatch(PlayerActorDraft current, PlayerActorCreationPatch patch) {
        if (patch == null) {
            return current;
        }
        Details mergedDetails = mergeDetails(current.details(), patch.details());
        return new PlayerActorDraft(
                StringUtils.firstNonBlank(patch.name(), current.name()),
                mergedDetails,
                StringUtils.firstNonBlank(patch.actorClass(), current.actorClass()),
                patch.level() != null ? patch.level() : current.level(),
                current.confirmed());
    }

    static Details mergeDetails(Details current, Details patch) {
        if (current == null) {
            return patch;
        }
        if (patch == null) {
            return current;
        }
        return new Draft.Details(
                StringUtils.firstNonBlank(patch.summary(), current.summary()),
                StringUtils.firstNonBlank(patch.description(), current.description()),
                patch.tags() != null ? patch.tags() : current.tags(),
                patch.aliases() != null ? patch.aliases() : current.aliases());
    }

    static String renderDraft(PlayerActorDraft draft) {
        if (draft == null) {
            return "No current draft.";
        }
        return "Current character draft:\n\n" + PlayerActor.Templates.playerActorDraft(draft).render();
    }
}
