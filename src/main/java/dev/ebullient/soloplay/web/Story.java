package dev.ebullient.soloplay.web;

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

        public static native TemplateInstance create();

        public static native TemplateInstance configure(StoryThread thread);

        public static native TemplateInstance play(StoryThread thread,
                java.util.List<dev.ebullient.soloplay.data.Character> partyMembers);
    }

    @Inject
    StoryRepository storyRepository;

    /**
     * Landing page - show all story threads with option to create new.
     */
    @GET
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
        return Templates.create();
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

        if (name == null || name.isBlank()) {
            flash("error", "Please provide a story thread name");
            return Templates.create();
        }

        if (settingName == null || settingName.isBlank()) {
            flash("error", "Please provide a setting name");
            return Templates.create();
        }

        StoryThread thread = new StoryThread(name, settingName);

        // Check if slug already exists - ask user to choose different name
        if (storyRepository.findStoryThreadBySlug(thread.getSlug()) != null) {
            flash("error", "A story with the name '" + name + "' already exists. Please choose a different name.");
            return Templates.create();
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
                return Templates.create();
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
}
