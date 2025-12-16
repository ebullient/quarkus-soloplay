package dev.ebullient.soloplay.web;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import dev.ebullient.soloplay.StoryRepository;
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

        public static native TemplateInstance create(List<String> availableSettings);

        public static native TemplateInstance configure(StoryThread thread);

        public static native TemplateInstance play(StoryThread thread,
                java.util.List<dev.ebullient.soloplay.data.Character> partyMembers);

        public static native TemplateInstance createCharacter(StoryThread thread);

        public static native TemplateInstance editCharacter(StoryThread thread,
                dev.ebullient.soloplay.data.Character character);
    }

    @Inject
    StoryRepository storyRepository;

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
     */
    @GET
    @Path("/create")
    public TemplateInstance create() {
        List<String> availableSettings = storyRepository.getAvailableSettings();
        return Templates.create(availableSettings);
    }

    /**
     * Handle story thread creation form submission.
     */
    @POST
    @Path("/create")
    public TemplateInstance createPost(
            @RestForm String name,
            @RestForm String settingName,
            @RestForm String adventureName,
            @RestForm String adventureDescription,
            @RestForm String followingMode) {

        List<String> availableSettings = storyRepository.getAvailableSettings();

        if (name == null || name.isBlank()) {
            flash("error", "Please provide a story thread name");
            return Templates.create(availableSettings);
        }

        if (settingName == null || settingName.isBlank()) {
            flash("error", "Please provide a setting name");
            return Templates.create(availableSettings);
        }

        // Validate that the setting exists in the RAG embedding store
        if (!availableSettings.contains(settingName)) {
            flash("error", "Setting '" + settingName
                    + "' not found. Please upload setting documents first or choose from available settings.");
            return Templates.create(availableSettings);
        }

        StoryThread thread = new StoryThread(name, settingName);

        // Check if slug already exists - ask user to choose different name
        if (storyRepository.findStoryThreadBySlug(thread.getSlug()) != null) {
            flash("error", "A story with the name '" + name + "' already exists. Please choose a different name.");
            return Templates.create(availableSettings);
        }

        // Set optional adventure fields
        if (adventureName != null && !adventureName.isBlank()) {
            thread.setAdventureName(adventureName);
        }
        if (adventureDescription != null && !adventureDescription.isBlank()) {
            thread.setAdventureDescription(adventureDescription);
        }
        if (followingMode != null && !followingMode.isBlank()) {
            try {
                thread.setFollowingMode(StoryThread.FollowingMode.valueOf(followingMode));
            } catch (IllegalArgumentException e) {
                flash("error", "Invalid following mode: " + followingMode);
                return Templates.create(availableSettings);
            }
        }
        storyRepository.saveStoryThread(thread);

        flash("success", "Story thread created: " + name);

        // Fetch party members for the new thread (will be empty initially)
        var partyMembers = storyRepository.findCharactersByAnyTag(
                thread.getSlug(),
                List.of("player-controlled", "companion"));

        return Templates.play(thread, partyMembers);
    }

    /**
     * Show configuration page for existing story thread.
     */
    @GET
    @Path("/{slug}/configure")
    public TemplateInstance configure(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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
            @RestForm String settingName,
            @RestForm String adventureName,
            @RestForm String adventureDescription,
            @RestForm String followingMode,
            @RestForm String status) {

        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
        if (thread == null) {
            flash("error", "Story thread not found");
            return select();
        }

        // Update fields (note: slug is immutable and cannot be changed)
        if (name != null && !name.isBlank()) {
            thread.setName(name);
        }
        if (settingName != null && !settingName.isBlank()) {
            thread.setSettingName(settingName);
        }
        if (adventureName != null) {
            thread.setAdventureName(adventureName.isBlank() ? null : adventureName);
        }
        if (adventureDescription != null) {
            thread.setAdventureDescription(adventureDescription.isBlank() ? null : adventureDescription);
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
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
        if (thread == null) {
            flash("error", "Story thread not found: " + slug);
            return select();
        }

        // Fetch party members (player-controlled and companions)
        var partyMembers = storyRepository.findCharactersByAnyTag(
                thread.getSlug(),
                List.of("player-controlled", "companion"));

        return Templates.play(thread, partyMembers);
    }

    /**
     * Show character creation form.
     */
    @GET
    @Path("/{slug}/character/create")
    public TemplateInstance createCharacter(@RestPath String slug) {
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
        if (thread == null) {
            flash("error", "Story thread not found");
            return select();
        }
        return Templates.createCharacter(thread);
    }

    /**
     * Handle character creation form submission.
     */
    @POST
    @Path("/{slug}/character/create")
    public TemplateInstance createCharacterPost(
            @RestPath String slug,
            @RestForm String name,
            @RestForm String summary,
            @RestForm String description,
            @RestForm String characterClass,
            @RestForm Integer level,
            @RestForm List<String> tags,
            @RestForm String customTags) {

        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
        if (thread == null) {
            flash("error", "Story thread not found");
            return select();
        }

        if (name == null || name.isBlank()) {
            flash("error", "Please provide a character name");
            return Templates.createCharacter(thread);
        }

        if (summary == null || summary.isBlank()) {
            flash("error", "Please provide a character summary");
            return Templates.createCharacter(thread);
        }

        // Build tag list - always include "player-controlled"
        List<String> allTags = new ArrayList<>();
        allTags.add("player-controlled");

        // Add checkbox tags
        if (tags != null && !tags.isEmpty()) {
            allTags.addAll(tags);
        }

        // Add custom tags (comma-separated)
        if (customTags != null && !customTags.isBlank()) {
            String[] customTagArray = customTags.split(",");
            for (String tag : customTagArray) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    allTags.add(trimmed);
                }
            }
        }

        // Create character
        var character = storyRepository.createCharacter(
                thread.getSlug(),
                name,
                summary,
                description,
                allTags);

        // Update optional fields
        if (characterClass != null && !characterClass.isBlank()) {
            character = storyRepository.updateCharacter(
                    character.getId(),
                    null, // name - don't change
                    null, // summary - don't change
                    null, // description - don't change
                    characterClass,
                    level);
        } else if (level != null) {
            character = storyRepository.updateCharacter(
                    character.getId(),
                    null, null, null,
                    null, // characterClass
                    level);
        }

        flash("success", "Character created: " + character.getName());
        return play(slug);
    }

    /**
     * Show character edit form.
     */
    @GET
    @Path("/{slug}/character/{characterId}/edit")
    public TemplateInstance editCharacter(@RestPath String slug, @RestPath String characterId) {
        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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

        StoryThread thread = storyRepository.findStoryThreadBySlug(slug);
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
}
