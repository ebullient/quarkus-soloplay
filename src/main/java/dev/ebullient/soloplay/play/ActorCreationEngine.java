package dev.ebullient.soloplay.play;

import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.StringUtils;
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

        game.setGamePhase(GamePhase.CHARACTER_CREATION);

        String trimmed = playerInput == null ? "" : playerInput.trim();
        if (GameEngine.isHelpCommand(trimmed)) {
            return help(game);
        }

        var currentDraft = getCurrentDraft(game);
        if ("/cancel".equalsIgnoreCase(trimmed) || "/reset".equalsIgnoreCase(trimmed)) {
            return resetDraft(game, trimmed);
        }
        if ("/draft".equalsIgnoreCase(trimmed)) {
            return GameResponse.reply(renderDraft(currentDraft));
        }
        if ("/confirm".equalsIgnoreCase(trimmed)) {
            return saveDraft(game, trimmed, currentDraft, emitter);
        }
        emitter.assistantDelta("The GM is thinking…\n");

        try {
            ActorCreationResponse response = handleAssistantResponse(game, currentDraft, trimmed);

            // All is well with parsed response
            var message = response.messageMarkdown();
            var patch = response.patch();

            emitter.assistantDelta("Updating your character…\n");
            PlayerActorDraft updatedDraft = applyPatch(currentDraft, patch);
            updateDraft(game, updatedDraft);

            return GameResponse.reply(
                    (message == null ? "ok." : message) + "\n\n"
                            + "\n\nUse `/draft` to review your character so far, or `/confirm` if this looks good to you.");
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = e.toString();
            }
            return GameResponse.error("Unable to get a response from the GM: " + message);
        }
    }

    private GameResponse saveDraft(GameState game,
            String trimmedInput,
            PlayerActorDraft draft,
            GameEventEmitter emitter) {
        emitter.assistantDelta("Confirming character…\n");

        String missing = missingRequired(draft);
        if (missing != null) {
            return GameResponse.error("Can't confirm yet: " + missing);
        }

        PlayerActor actor = new PlayerActor(game.getGameId(), draft);
        emitter.assistantDelta("Saving character…\n");
        gameRepository.saveActor(actor);
        cleanupDraft(game);

        game.setGamePhase(game.getGamePhase().next());
        return GameResponse.reply("""
                Created your character: **%s** (%s, level %s).

                Use `/newcharacter` to create an additional character, or `/start` to start or resume your game.
                """.stripIndent().formatted(actor.getName(), actor.getActorClass(), actor.getLevel()));
    }

    private GameResponse resetDraft(GameState game, String trimmedInput) {
        cleanupDraft(game);
        if ("/cancel".equalsIgnoreCase(trimmedInput)) {
            String partyMembers = gameRepository.listPlayerActors(game.getGameId()).stream()
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

    private ActorCreationResponse handleAssistantResponse(GameState game,
            PlayerActorDraft currentDraft,
            String playerInput) {
        String chatMemoryId = game.getGameId() + "-character";

        if ((playerInput.isBlank() || playerInput.equals("/start")) && currentDraft == EMPTY_DRAFT) {
            return assistant.start(chatMemoryId, game.getGameId(), game.getAdventureName());
        } else {
            return assistant.step(chatMemoryId, game.getGameId(), game.getAdventureName(), currentDraft,
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
