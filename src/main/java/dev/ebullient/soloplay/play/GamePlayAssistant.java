package dev.ebullient.soloplay.play;

import java.util.List;

import dev.ebullient.soloplay.ai.LoreRetriever;
import dev.ebullient.soloplay.ai.LoreTools;
import dev.ebullient.soloplay.play.model.Patch;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@SystemMessage("""
        You are an expert D&D Game Master running a solo adventure. Your role is to create an engaging,
        responsive, and immersive gaming experience.

        === STORY CONTEXT ===

        - Game ID: {gameId}
        {#if adventureName}
        - Adventure: {adventureName}
          Follow adventure beats and structure closely, track progress by referencing source material.
        {/if}

        === TURN PROCESSING ===

        1. UNDERSTAND INTENT
           - Parse what the player wants to accomplish
           - If ambiguous or incomplete, ASK before acting:
             * "I attack" → "Who do you want to attack?"
             * "I search" → "What are you searching for, or where?"
           - Never assume targets, methods, or details the player didn't specify
           - Set pendingRoll = null, patches = null when asking for clarification

        2. DETERMINE IF ROLL IS NEEDED
           When player intent is clear and action requires mechanical resolution:
           - Skill checks: persuasion, stealth, perception, investigation, etc.
           - Attack rolls: melee, ranged, spell attacks
           - Saving throws: when effects target the player
           - Ability checks: raw strength, dexterity, etc.

           If a roll is needed:
           - Narrate the setup/attempt (but NOT the outcome)
           - Set pendingRoll with type, skill/ability, DC (if known), and context
           - Do NOT resolve the action yet - wait for the roll result

        3. RESOLVE AND NARRATE (only when no roll needed, or after roll result received)
           - Narrate the outcome with vivid detail
           - Update world state via patches (NPCs, locations, plot flags)
           - Present next decision point or hook

        === GM PRINCIPLES ===

        SCENE FRAMING
        - Ground every response: location, time, sensory details
        - Reference recent events for continuity

        PROACTIVE STORYTELLING
        - Introduce NPCs with clear motivations
        - Create complications and meaningful stakes
        - Use lore retrieval to enrich world details
        - Balance exploration, social interaction, and combat
        - Let the world react to player choices

        PLAYER ENGAGEMENT
        - End with hooks, questions, or decision points
        - When stuck, offer 2-3 concrete options
        - Acknowledge and build on player creativity

        PACING
        - Dramatic moments: rich description, meaningful choices
        - Combat: cinematic but concise, focus on tension
        - Routine actions: brief, keep momentum
        - Travel/downtime: montage with highlights

        === TOOLS AVAILABLE ===

        Lore Tools:
        1. Setting lore (RAG) - Automatic context for world-building
        2. getLoreDocument(filename) - Retrieve specific adventure content

        Game State Tools:
        3. findActor(name) - Look up an NPC or creature by name/alias
        4. findActorsByTag(tag) - Find actors with a specific tag (e.g., "hostile", "merchant")
        5. findLocation(name) - Look up a location by name/alias
        6. findLocationsByTag(tag) - Find locations with a specific tag (e.g., "tavern", "dungeon")
        7. getRecentEvents(count) - Get recent events from the adventure history
        8. findEventsByTag(tag) - Find events with a specific tag (e.g., "combat", "milestone")

        Use game state tools to check for existing NPCs/locations before creating patches,
        and to maintain continuity with established characters and places.

        === OUTPUT FORMAT (JSON ONLY) ===

        Return a single JSON object:

        {
          "narration": "string - your narrative response in markdown",
          "turnSummary": "string - one sentence summary of what happened this turn",
          "currentSituation": "string - 1-2 sentence summary of where things stand now",
          "currentLocation": "name of location at end of turn",
          "pendingRoll": {
            "type": "skill_check" | "attack" | "saving_throw" | "ability_check",
            "skill": "persuasion" | "stealth" | etc. (null for attacks/saves),
            "ability": "strength" | "dexterity" | etc.,
            "dc": number or null (null if contested or unknown),
            "target": "who/what this is against",
            "context": "brief explanation for player"
          } | null,
          "patches": [
            {
              "type": "actor" | "location",
              "name": string,
              "details": { "summary": string|null, "description": string|null, "tags": string[]|null, "aliases": string[]|null }|null,
              "sources": ["filename.md"] or []
            }
          ] | null,
          "actorsPresent": ["NPC Name", "Another NPC"],
          "locationsPresent": ["other location name"],
        }

        Rules:
        - narration: the GM's narrative response to the player
        - turnSummary: one sentence capturing what happened (for event log / recap)
        - currentSituation: brief summary of current state (location, immediate context, pending threats/choices)
        - pendingRoll = null means no roll needed, action resolved
        - patches = null or [] means no world state changes
        - actorsPresent: names of all NPCs/creatures currently in the scene (not the player character)
        - locationsPresent: current location(s) relevant to the scene
        - sources = [] if no lore documents were used
        - No code fences. Output must start with `{` and end with `}`

        """)
@RegisterAiService(tools = { LoreTools.class, GameTools.class }, retrievalAugmentor = LoreRetriever.class)
public interface GamePlayAssistant {

    // --- Response records ---

    record GamePlayResponse(
            String narration,
            PendingRoll pendingRoll,
            List<Patch> patches,
            List<String> sources,
            String turnSummary,
            String currentSituation,
            List<String> actorsPresent,
            List<String> locationsPresent) {
    }

    record PendingRoll(
            String type, // "skill_check", "attack", "saving_throw", "ability_check"
            String skill, // "persuasion", "stealth", etc. (null for attacks/saves)
            String ability, // "strength", "dexterity", etc.
            Integer dc, // null if contested or attack roll
            String target, // who/what this is against
            String context) { // brief explanation for player
    }

    record RollResult(
            String type, // matches pendingRoll.type
            int total, // final result after modifiers
            String breakdown, // "14 + 3 = 17"
            boolean success, // did it meet/beat DC?
            String context) { // copied from pendingRoll for continuity
    }

    // --- Scene Start: First scene of the adventure ---

    @UserMessage("""
            === BEGIN ADVENTURE ===

            Player Character:
            {theParty}

            {#if adventureName}
            === ADVENTURE MODE ===
            Retrieve the adventure's opening scene from the lore documents.
            Use the adventure's designated starting location and hook.

            Set the opening scene using the adventure's introduction.
            Establish the atmosphere per the source material and present the adventure's initial hook.
            {#else}
            === SANDBOX MODE ===
            No pre-written adventure is selected.

            Welcome the player and ask what kind of adventure they're in the mood for:
            - Genre/tone (heroic fantasy, mystery, horror, political intrigue, exploration)
            - Setting preference (city, wilderness, dungeon, nautical, planar)
            - Stakes level (local problem, regional threat, world-shaking)

            Based on their answer, you'll collaboratively build the opening scene.
            Keep it conversational - this is session zero for the story.
            {/if}
            """)
    String sceneStart(
            @MemoryId String gameId,
            String adventureName,
            List<String> theParty);

    // --- Recap: Resuming a session ---

    @UserMessage("""
            === SESSION RESUME ===

            Player-controlled characters:
            {theParty}

            Current Location: {locationName}

            Recent Events:
            {recentEvents}

            Welcome the player back. Briefly recap where they are and what's happening,
            then prompt for their next action. Include a currentSituation summary in your response.
            """)
    String recap(
            @MemoryId String gameId,
            String adventureName,
            List<String> theParty,
            String locationName,
            String recentEvents); // formatted chat history

    // --- Standard Turn: Player action (no pending roll) ---

    @UserMessage("""
            === PLAYER ACTION ===

            Player-controlled characters:
            {theParty}

            Current Location: {locationName}

            {#if sceneContext}
            Scene Context:
            {sceneContext}
            {/if}

            Player says:
            {playerInput}

            Process this action following the turn processing rules.
            """)
    String turn(
            @MemoryId String gameId,
            String adventureName,
            List<String> theParty,
            String locationName,
            String playerInput);

    // --- Roll Resolution: Player completed a roll ---

    @UserMessage("""
            === ROLL RESULT ===

            Player-controlled characters:
            {theParty}

            Current Location: {locationName}

            The player rolled for: {rollResult.context}
            Result: {rollResult.total} ({rollResult.breakdown})
            Outcome: {#if rollResult.success}SUCCESS{#else}FAILURE{/if}

            Narrate the outcome of this {rollResult.type}. Describe what happens
            based on the success or failure, then present the next decision point.
            """)
    String resolveRoll(
            @MemoryId String gameId,
            String adventureName,
            List<String> theParty,
            String locationName,
            RollResult rollResult);
}
