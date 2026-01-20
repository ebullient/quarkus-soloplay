package dev.ebullient.soloplay.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import dev.ebullient.soloplay.LoreRepository;
import dev.ebullient.soloplay.StoryRepository;
import dev.ebullient.soloplay.ai.CharacterCreatorService;
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
                List<Character> partyMembers);

        public static native TemplateInstance createCharacter(StoryThread thread, String initialGreeting);

        public static native TemplateInstance editCharacter(StoryThread thread,
                Character character);
    }

    @Inject
    StoryRepository storyRepository;

    @Inject
    LoreRepository loreRepository;

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
     * Redirects to the play page on success to ensure URL matches the page.
     */
    @POST
    @Path("/create")
    public void createPost(
            @RestForm String name,
            @RestForm String adventureName,
            @RestForm String followingMode) {

        if (name == null || name.isBlank()) {
            flash("error", "Please provide a story thread name");
            create();
            return;
        }

        StoryThread thread;
        try {
            thread = storyRepository.createStoryThread(name, adventureName, followingMode);
        } catch (IllegalArgumentException e) {
            flash("error", e.getMessage());
            create();
            return;
        }

        flash("success", "Story thread created: " + name);
        redirect(Story.class).play(thread.getId());
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
     * WebSocket handles all GM interactions - no server-side greeting generation.
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

        return Templates.play(thread, partyMembers);
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

        // Build new tag set from form selections
        Set<String> allTags = new HashSet<>();
        if (tags != null) {
            allTags.addAll(tags);
        }

        if (customTags != null && !customTags.isBlank()) {
            for (String tag : customTags.split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    allTags.add(trimmed);
                }
            }
        }

        storyRepository.setCharacterTags(characterId, allTags);

        flash("success", "Character updated: " + character.getName());
        return play(slug);
    }
}
