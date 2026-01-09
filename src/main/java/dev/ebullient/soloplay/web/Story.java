package dev.ebullient.soloplay.web;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import dev.ebullient.soloplay.LoreRepository;
import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.ai.CharacterCreatorService;
import dev.ebullient.soloplay.ai.GameMasterService;
import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.StoryThread;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

/**
 * Renarde controller for story thread selection and management.
 * Provides the landing page for solo play where users select or create story threads.
 */
@Path("/story")
public class Story extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance select(List<StoryThread> threads);

        public static native TemplateInstance create(List<String> adventures);

        public static native TemplateInstance configure(StoryThread thread);

        public static native TemplateInstance play(StoryThread thread,
                List<Character> partyMembers,
                String gmGreeting);

        public static native TemplateInstance createCharacter(StoryThread thread, String initialGreeting);

        public static native TemplateInstance editCharacter(StoryThread thread,
                Character character);
    }

    @Inject
    StoryRepository storyRepository;

    @Inject
    LoreRepository loreRepository;

    @Inject
    GameMasterService gameMaster;

    @Inject
    CharacterCreatorService characterCreator;

    /**
     * Landing page - show all story threads with option to create new.
     */
    @GET
    @Path("/")
    public TemplateInstance select() {
        List<StoryThread> threads = storyRepository.findAllStoryThreads();
        return Templates.select(threads);
    }

    /**
     * Show create new story thread form.
     * Pre-populates adventure dropdown with discovered adventures from lore.
     */
    @GET
    @Path("/create")
    public TemplateInstance create() {
        List<String> adventures = loreRepository.listAdventures();
        return Templates.create(adventures);
    }

    /**
     * Handle story thread creation form submission.
     */
    @POST
    @Path("/create")
    public TemplateInstance createPost(
            @RestForm String name,
            @RestForm String adventureName,
            @RestForm String followingMode) {

        if (name == null || name.isBlank()) {
            flash("error", "Please provide a story thread name");
            List<String> adventures = loreRepository.listAdventures();
            return Templates.create(adventures);
        }

        StoryThread thread;
        try {
            thread = storyRepository.createStoryThread(name, adventureName, followingMode);
        } catch (IllegalArgumentException e) {
            flash("error", e.getMessage());
            List<String> adventures = loreRepository.listAdventures();
            return Templates.create(adventures);
        }

        flash("success", "Story thread created: " + name);

        // Fetch party members for the new thread (will be empty initially)
        var partyMembers = storyRepository.findCharactersByAnyTag(
                thread.getId(),
                List.of("player-controlled", "companion"));

        return Templates.play(thread, partyMembers, null);
    }

    /**
     * Show configuration page for existing story thread.
     */
    @GET
    @Path("/{slug}/configure")
    public TemplateInstance configure(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            flash("error", "Story thread not found");
            return select();
        }
        return Templates.configure(thread);
    }

    /**
     * Handle story thread configuration update.
     * Note: Slug cannot be changed (it's the primary ID).
     */
    @POST
    @Path("/{slug}/configure")
    public TemplateInstance configurePost(
            @RestPath String slug,
            @RestForm String name,
            @RestForm String adventureName,
            @RestForm String followingMode,
            @RestForm String status) {

        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            flash("error", "Story thread not found");
            return select();
        }

        // Update fields (note: slug is immutable and cannot be changed)
        if (name != null && !name.isBlank()) {
            thread.setName(name);
        }
        if (adventureName != null) {
            thread.setAdventureName(adventureName.isBlank() ? null : adventureName);
        }
        if (followingMode != null && !followingMode.isBlank()) {
            try {
                thread.setFollowingMode(StoryThread.FollowingMode.valueOf(followingMode));
            } catch (IllegalArgumentException e) {
                flash("error", "Invalid following mode: " + followingMode);
                return Templates.configure(thread);
            }
        }
        if (status != null && !status.isBlank()) {
            try {
                thread.setStatus(StoryThread.StoryStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                flash("error", "Invalid status: " + status);
                return Templates.configure(thread);
            }
        }
        storyRepository.saveStoryThread(thread);

        flash("success", "Story thread updated: " + thread.getName());
        return select();
    }

    /**
     * Main play interface.
     */
    @GET
    @Path("/{slug}/play")
    public TemplateInstance play(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            flash("error", "Story thread not found: " + slug);
            return select();
        }

        // Fetch party members (player-controlled and companions)
        var partyMembers = storyRepository.findCharactersByAnyTag(
                thread.getId(),
                List.of("player-controlled", "companion"));

        // Generate initial GM greeting if there are characters but no events yet
        String gmGreeting = null;
        if (!partyMembers.isEmpty()) {
            var recentEvents = storyRepository.findRecentEvents(thread.getId(), 1);
            if (recentEvents.isEmpty()) {
                // First time playing with this character - get GM to start the adventure
                String initialPrompt = buildInitialPrompt(thread);
                gmGreeting = gameMaster.chat(thread.getId(), initialPrompt);
            }
        }

        return Templates.play(thread, partyMembers, gmGreeting);
    }

    /**
     * Show character creation chat interface.
     * Generates an initial greeting from the character creator assistant.
     */
    @GET
    @Path("/{slug}/character/create")
    public TemplateInstance createCharacter(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            flash("error", "Story thread not found");
            return select();
        }

        // Generate initial greeting from character creator
        String initialGreeting = characterCreator.getInitialGreeting(thread.getId());

        return Templates.createCharacter(thread, initialGreeting);
    }

    /**
     * Show character edit form.
     */
    @GET
    @Path("/{slug}/character/{characterId}/edit")
    public TemplateInstance editCharacter(@RestPath String slug, @RestPath String characterId) {
        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            flash("error", "Story thread not found");
            return select();
        }

        var character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            flash("error", "Character not found");
            return play(slug);
        }

        return Templates.editCharacter(thread, character);
    }

    /**
     * Handle character edit form submission.
     */
    @POST
    @Path("/{slug}/character/{characterId}/edit")
    public TemplateInstance editCharacterPost(
            @RestPath String slug,
            @RestPath String characterId,
            @RestForm String name,
            @RestForm String summary,
            @RestForm String description,
            @RestForm String characterClass,
            @RestForm Integer level,
            @RestForm List<String> tags,
            @RestForm String customTags) {

        StoryThread thread = storyRepository.findStoryThreadById(slug);
        if (thread == null) {
            flash("error", "Story thread not found");
            return select();
        }

        var character = storyRepository.findCharacterById(characterId);
        if (character == null) {
            flash("error", "Character not found");
            return play(slug);
        }

        if (name == null || name.isBlank()) {
            flash("error", "Please provide a character name");
            return Templates.editCharacter(thread, character);
        }

        if (summary == null || summary.isBlank()) {
            flash("error", "Please provide a character summary");
            return Templates.editCharacter(thread, character);
        }

        // Update character
        character = storyRepository.updateCharacter(
                characterId,
                name,
                summary,
                description,
                characterClass,
                level);

        // Update tags - remove all existing tags and add new ones
        // First, get current tags to remove them all
        var currentTags = new ArrayList<>(character.getTags());
        if (!currentTags.isEmpty()) {
            storyRepository.removeCharacterTags(characterId, currentTags);
        }

        // Build new tag list - always include "player-controlled" for player characters
        List<String> allTags = new ArrayList<>();
        allTags.add("player-controlled");

        if (tags != null && !tags.isEmpty()) {
            allTags.addAll(tags);
        }

        if (customTags != null && !customTags.isBlank()) {
            String[] customTagArray = customTags.split(",");
            for (String tag : customTagArray) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    allTags.add(trimmed);
                }
            }
        }

        storyRepository.addCharacterTags(characterId, allTags);

        flash("success", "Character updated: " + character.getName());
        return play(slug);
    }

    /**
     * Build the initial prompt for the GM based on the story thread configuration.
     * Adjusts the prompt based on adventure name and following mode.
     */
    private String buildInitialPrompt(StoryThread thread) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("The player has just created their character and is ready to begin the adventure. ");

        if (thread.getAdventureName() != null && !thread.getAdventureName().isBlank()) {
            String followingMode = thread.getFollowingMode() != null ? thread.getFollowingMode().toString() : "LOOSE";

            switch (followingMode) {
                case "STRICT":
                    prompt.append("This is the published adventure '")
                            .append(thread.getAdventureName())
                            .append("'. ");
                    prompt.append("Look up the adventure's opening scene and starting hook from the source material. ");
                    prompt.append(
                            "Present the opening exactly as written in the adventure, establishing the initial situation, ");
                    prompt.append("location, and hook that draws the characters into the story. ");
                    prompt.append(
                            "Set the scene following the adventure's structure and ask the player what they'd like to do.");
                    break;

                case "INSPIRATION":
                    prompt.append("The adventure '")
                            .append(thread.getAdventureName())
                            .append("' is available as reference material, but don't use it yet. ");
                    prompt.append("Set an appropriate opening scene for a D&D adventure and welcome the player. ");
                    prompt.append("Ask them what they'd like to do. ");
                    prompt.append("(You can reference the adventure later if the player asks.)");
                    break;

                case "LOOSE":
                default:
                    prompt.append("This is the published adventure '")
                            .append(thread.getAdventureName())
                            .append("'. ");
                    prompt.append("Look up the adventure's opening scene and hook from the source material. ");
                    prompt.append("Use this as your starting point, but feel free to adapt the presentation and details. ");
                    prompt.append("Capture the spirit of the adventure while remaining flexible for player-driven choices. ");
                    prompt.append("Set the opening scene and ask the player what they'd like to do.");
                    break;
            }
        } else {
            // No adventure specified - sandbox/homebrew
            prompt.append("This is a sandbox adventure with no specific module. ");
            prompt.append("Set an engaging opening scene appropriate for the setting and characters. ");
            prompt.append("Welcome the player to the story and ask them what they'd like to do.");
        }

        return prompt.toString();
    }
}
