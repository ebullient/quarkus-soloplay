package dev.ebullient.soloplay.play;

import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.StringUtils;
import dev.ebullient.soloplay.play.ActorCreationResponseGuardrail.ActorCreationResponse;
import dev.ebullient.soloplay.play.model.GameState;
import dev.ebullient.soloplay.play.model.GameState.GamePhase;
import dev.ebullient.soloplay.play.model.PlayerActor;
import dev.ebullient.soloplay.play.model.PlayerActorCreationPatch;
import dev.ebullient.soloplay.play.model.PlayerActorDraft;

@ApplicationScoped
public class ActorCreationEngine {
    static final PlayerActorDraft EMPTY_DRAFT = new PlayerActorDraft(null, null, null, null, null, null, null, false);
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
            return GameResponse.reply("Ok — cleared your character draft.");
        }
        if ("/draft".equalsIgnoreCase(trimmed)) {
            return GameResponse.reply(renderDraft(currentDraft));
        }
        if ("/confirm".equalsIgnoreCase(trimmed)) {
            emitter.assistantDelta("Confirming character…\n");

            String missing = missingRequired(currentDraft);
            if (missing != null) {
                return GameResponse.error("Can't confirm yet: " + missing);
            }

            PlayerActor actor = new PlayerActor(gameId, currentDraft);
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

        ActorCreationResponseGuardrail.ActorCreationResponse response = null;
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
                (message == null ? "ok." : message) + "\n\n"
                        + "\n\nUse `/draft` to review your character so far, or `/confirm` if this looks good to you.");
    }

    private ActorCreationResponse handleAssistantResponse(GameState state,
            PlayerActorDraft currentDraft,
            String playerInput) {
        String chatMemoryId = state.getGameId() + "-character";

        if ((playerInput.isBlank() || playerInput.equals("/start")) && currentDraft == EMPTY_DRAFT) {
            return assistant.start(chatMemoryId, state.getGameId(), state.getAdventureName());
        } else {
            return assistant.turn(chatMemoryId, state.getGameId(), state.getAdventureName(), currentDraft,
                    playerInput);
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
        return game.getStashOrDefault(DRAFT_KEY, PlayerActorDraft.class, EMPTY_DRAFT);
    }

    void cleanupDraft(GameState game) {
        game.removeStash(DRAFT_KEY);
    }

    void updateDraft(GameState game, PlayerActorDraft updatedDraft) {
        game.putStash(DRAFT_KEY, updatedDraft);
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
        return new PlayerActorDraft(
                StringUtils.firstNonBlank(patch.name(), current.name()),
                StringUtils.firstNonBlank(patch.actorClass(), current.actorClass()),
                patch.level() != null ? patch.level() : current.level(),
                StringUtils.firstNonBlank(patch.summary(), current.summary()),
                StringUtils.firstNonBlank(patch.description(), current.description()),
                patch.tags() != null ? patch.tags() : current.tags(),
                patch.aliases() != null ? patch.aliases() : current.aliases(),
                current.confirmed());
    }

    static String renderDraft(PlayerActorDraft draft) {
        if (draft == null) {
            return "No current draft.";
        }
        return "Current character draft:\n\n" + PlayerActor.Templates.playerActorDraft(draft).render();
    }
}
