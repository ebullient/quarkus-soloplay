package dev.ebullient.soloplay.play;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ebullient.soloplay.GameRepository;
import dev.ebullient.soloplay.play.GamePlayAssistant.GamePlayResponse;
import dev.ebullient.soloplay.play.GamePlayAssistant.PendingRoll;
import dev.ebullient.soloplay.play.GamePlayAssistant.RollResult;
import dev.ebullient.soloplay.play.model.Actor;
import dev.ebullient.soloplay.play.model.BaseEntity;
import dev.ebullient.soloplay.play.model.GameState;
import dev.ebullient.soloplay.play.model.Location;
import dev.ebullient.soloplay.play.model.Patch;
import dev.ebullient.soloplay.play.model.PlayerActor;
import io.quarkus.logging.Log;

@ApplicationScoped
public class GamePlayEngine {

    @Inject
    GameRepository gameRepository;

    @Inject
    GamePlayAssistant assistant;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RollHandler rollHandler;

    public GameResponse sceneStart(GameState game, GameEventEmitter emitter) {
        emitter.assistantDelta("Setting the scene…\n");

        String rawResponse = assistant.sceneStart(
                game.getGameId(),
                game.getAdventureName(),
                listTheParty(game));

        return processResponse(game, parseResponse(rawResponse), emitter);
    }

    public GameResponse recap(GameState game, String recentEvents, GameEventEmitter emitter) {
        emitter.assistantDelta("Recapping the story…\n");

        String rawResponse = assistant.recap(
                game.getGameId(),
                game.getAdventureName(),
                listTheParty(game),
                game.getCurrentLocation(),
                recentEvents);

        return processResponse(game, parseResponse(rawResponse), emitter);
    }

    public GameResponse processRequest(GameState game, String playerInput, GameEventEmitter emitter) {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(emitter, "emitter");

        String trimmed = playerInput == null ? "" : playerInput.trim();

        // Check for pending roll resolution
        PendingRoll pendingRoll = rollHandler.getPendingRoll(game);
        if (pendingRoll != null && isRollInput(trimmed)) {
            return resolveRoll(game, pendingRoll, trimmed, emitter);
        }

        // Standard turn
        return handleTurn(game, trimmed, emitter);
    }

    private GameResponse handleTurn(GameState game, String playerInput, GameEventEmitter emitter) {
        emitter.assistantDelta("The GM is thinking…\n");

        // TODO: gather context for the turn
        String rawResponse = assistant.turn(
                game.getGameId(),
                game.getAdventureName(),
                listTheParty(game),
                game.getCurrentLocation(),
                playerInput);

        return processResponse(game, parseResponse(rawResponse), emitter);
    }

    private GameResponse resolveRoll(GameState game, PendingRoll pending, String rollInput,
            GameEventEmitter emitter) {
        emitter.assistantDelta("Processing roll…\n");

        RollResult rollResult = rollHandler.handleRollCommand(game, rollInput);
        if (rollResult == null) {
            return GameResponse.error("Could not parse roll input: " + rollInput);
        }

        rollHandler.clearPendingRoll(game);

        String rawResponse = assistant.resolveRoll(
                game.getGameId(),
                game.getAdventureName(),
                listTheParty(game),
                game.getCurrentLocation(),
                rollResult);

        return processResponse(game, parseResponse(rawResponse), emitter);
    }

    GamePlayResponse parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            Log.error("Empty response from assistant");
            throw new AssistantResponseException("Empty response from assistant", true);
        }

        Log.debugf("Response received from LLM: %s", rawResponse);

        try {
            GamePlayResponse response = objectMapper.readValue(rawResponse, GamePlayResponse.class);
            if (response.narration() == null) {
                Log.errorf("Narration was missing. Raw: %s", rawResponse);
                throw new AssistantResponseException("Narration was missing", true);
            }
            return response;
        } catch (JsonParseException | JsonMappingException e) {
            Log.errorf(e, "Malformed JSON from assistant. Raw: %s", rawResponse);
            throw new AssistantResponseException("Malformed JSON: " + e.getOriginalMessage(), e, true);
        } catch (Exception e) {
            Log.errorf(e, "Failed to parse response. Raw: %s", rawResponse);
            throw new AssistantResponseException("Parse error: " + e.getMessage(), e, false);
        }
    }

    private GameResponse processResponse(GameState game, GamePlayResponse response, GameEventEmitter emitter) {
        if (response == null || response.narration() == null) {
            return GameResponse.error("No response from GM");
        }

        // Apply patches (actors, locations, plot flags)
        if (response.patches() != null && !response.patches().isEmpty()) {
            emitter.assistantDelta("Updating world state…\n");
            applyPatches(game, response.patches());
        }

        // Store pending roll if present
        if (response.pendingRoll() != null) {
            emitter.assistantDelta("Tracking pending roll…\n");
            storePendingRoll(game, response.pendingRoll());
        }

        return GameResponse.reply(response.narration());
    }

    private void storePendingRoll(GameState game, PendingRoll roll) {
        PendingRoll draft = new PendingRoll(
                roll.type(),
                roll.skill(),
                roll.ability(),
                roll.dc(),
                roll.target(),
                roll.context());
        rollHandler.setPendingRoll(game, draft);
    }

    private boolean isRollInput(String input) {
        // TODO: detect roll commands or dice notation
        // e.g., "/roll", "1d20+5", "15", etc.
        return input.startsWith("/roll") || input.matches("\\d+");
    }

    private void applyPatches(GameState game, List<Patch> patches) {
        List<BaseEntity> modified = new ArrayList<>();

        for (Patch patch : patches) {
            switch (patch.type()) {
                case "actor" -> {
                    var merged = handleActor(game, patch);
                    modified.add(merged);
                }
                case "location" -> {
                    var merged = handleLocation(game, patch);
                    modified.add(merged);
                }
            }
        }

        gameRepository.saveAll(modified); // single TX
    }

    Actor handleActor(GameState game, Patch p) {
        var actor = gameRepository.findActorByNameOrAlias(game.getGameId(), p.name());
        if (actor == null) {
            return new Actor(game.getGameId(), p);
        }
        if (actor instanceof PlayerActor playerActor) {
            // preserve extra player actor attributes
            return playerActor.merge(p);
        }
        return actor.merge(p);
    }

    Location handleLocation(GameState game, Patch p) {
        var location = gameRepository.findLocationByNameOrAlias(game.getGameId(), p.name());
        if (location == null) {
            return new Location(game.getGameId(), p);
        }
        return location.merge(p);
    }

    List<String> listTheParty(GameState game) {
        return gameRepository.findTheParty(game.getGameId())
                .stream().map(a -> a.getName()).toList();
    }
}
