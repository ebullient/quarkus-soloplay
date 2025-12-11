package dev.ebullient.soloplay.web;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import dev.ebullient.soloplay.CampaignRepository;
import dev.ebullient.soloplay.CampaignTools;
import dev.ebullient.soloplay.data.CampaignEvent;
import dev.ebullient.soloplay.data.Character;
import dev.ebullient.soloplay.data.CharacterRelationship;
import dev.ebullient.soloplay.data.Location;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

/**
 * Controller for viewing and managing campaign data.
 * Provides "Inspect" interface - CRUD views over characters, locations, events, and relationships.
 * This allows us to oversee what the AI is working with in the campaign database,
 * but.. SPOLIERS!
 */
@Path("/inspect")
public class CampaignData extends Controller {

    @Inject
    CampaignRepository campaignRepository;

    @Inject
    CampaignTools campaignTools;

    @CheckedTemplate
    public static class Templates {
        // Main dashboard
        public static native TemplateInstance index(String campaignId);

        // Character views
        public static native TemplateInstance characters(List<Character> characters, String campaignId);

        public static native TemplateInstance characterDetail(Character character);

        // Location views
        public static native TemplateInstance locations(List<Location> locations, String campaignId);

        public static native TemplateInstance locationDetail(Location location);

        // Relationship views
        public static native TemplateInstance relationships(String campaignId, List<CharacterRelationship> relationships);

        public static native TemplateInstance characterRelationships(Character character,
                List<CharacterRelationship> relationships);

        public static native TemplateInstance locationConnections(Location location, List<Character> connectedCharacters);

        public static native TemplateInstance sharedHistory(Character char1, Character char2, List<CampaignEvent> sharedEvents);

        // AI Tool outputs (what the AI sees)
        public static native TemplateInstance aiToolOutput(String toolName, String output);
    }

    /**
     * Main campaign data dashboard.
     * Shows overview of all campaign elements.
     */
    @GET
    @Path("/")
    public TemplateInstance index(@RestQuery String campaignId) {
        if (campaignId == null || campaignId.isBlank()) {
            campaignId = "default";
        }
        return Templates.index(campaignId);
    }

    /**
     * List all characters in a campaign.
     */
    @GET
    @Path("/characters")
    public TemplateInstance characters(@RestQuery String campaignId) {
        if (campaignId == null || campaignId.isBlank()) {
            campaignId = "default";
        }
        List<Character> characters = campaignRepository.findCharactersByCampaignIdOrderByCreatedAt(campaignId);
        return Templates.characters(characters, campaignId);
    }

    /**
     * View a single character's details.
     */
    @GET
    @Path("/characters/{id}")
    public TemplateInstance characterDetail(@RestPath String id) {
        Character character = campaignRepository.findCharacterByIdForWeb(id);
        if (character == null) {
            notFound();
            return null; // This line is never reached, but needed for compilation
        }
        return Templates.characterDetail(character);
    }

    /**
     * List all locations in a campaign.
     */
    @GET
    @Path("/locations")
    public TemplateInstance locations(@RestQuery String campaignId) {
        if (campaignId == null || campaignId.isBlank()) {
            campaignId = "default";
        }
        List<Location> locations = campaignRepository.findLocationsByCampaignIdOrderByCreatedAt(campaignId);
        return Templates.locations(locations, campaignId);
    }

    /**
     * View a single location's details.
     */
    @GET
    @Path("/locations/{id}")
    public TemplateInstance locationDetail(@RestPath String id) {
        Location location = campaignRepository.findLocationByIdForWeb(id);
        if (location == null) {
            notFound();
            return null; // This line is never reached, but needed for compilation
        }
        return Templates.locationDetail(location);
    }

    /**
     * View all relationships in a campaign (relationship network).
     */
    @GET
    @Path("/relationships")
    public TemplateInstance relationships(@RestQuery String campaignId) {
        if (campaignId == null || campaignId.isBlank()) {
            campaignId = "default";
        }
        List<CharacterRelationship> relationships = campaignRepository.findRelationshipsByCampaignId(campaignId);
        return Templates.relationships(campaignId, relationships);
    }

    /**
     * View relationships for a specific character.
     */
    @GET
    @Path("/characters/{id}/relationships")
    public TemplateInstance characterRelationships(@RestPath String id) {
        Character character = campaignRepository.findCharacterByIdForWeb(id);
        if (character == null) {
            notFound();
            return null;
        }
        List<CharacterRelationship> relationships = campaignRepository.findRelationshipsByCharacterId(id);
        return Templates.characterRelationships(character, relationships);
    }

    /**
     * View characters connected to a location.
     */
    @GET
    @Path("/locations/{id}/connections")
    public TemplateInstance locationConnections(@RestPath String id) {
        Location location = campaignRepository.findLocationByIdForWeb(id);
        if (location == null) {
            notFound();
            return null;
        }
        List<Character> connectedCharacters = campaignRepository.findCharactersByLocation(id);
        return Templates.locationConnections(location, connectedCharacters);
    }

    /**
     * View shared history between two characters.
     */
    @GET
    @Path("/shared-history")
    public TemplateInstance sharedHistory(@RestQuery String char1Id, @RestQuery String char2Id) {
        if (char1Id == null || char2Id == null) {
            badRequest();
            return null;
        }
        Character char1 = campaignRepository.findCharacterByIdForWeb(char1Id);
        Character char2 = campaignRepository.findCharacterByIdForWeb(char2Id);
        if (char1 == null || char2 == null) {
            notFound();
            return null;
        }
        List<CampaignEvent> sharedEvents = campaignRepository.findSharedEvents(char1Id, char2Id);
        return Templates.sharedHistory(char1, char2, sharedEvents);
    }

    // === AI TOOL OUTPUT INSPECTION ===

    /**
     * See what the AI tool returns for character relationships.
     */
    @GET
    @Path("/ai/character-relationships/{id}")
    public TemplateInstance aiCharacterRelationships(@RestPath String id) {
        String output = campaignTools.getCharacterRelationships(id);
        return Templates.aiToolOutput("Character Relationships", output);
    }

    /**
     * See what the AI tool returns for campaign network.
     */
    @GET
    @Path("/ai/campaign-network")
    public TemplateInstance aiCampaignNetwork(@RestQuery String campaignId) {
        if (campaignId == null || campaignId.isBlank()) {
            campaignId = "default";
        }
        String output = campaignTools.getCampaignNetwork(campaignId);
        return Templates.aiToolOutput("Campaign Network", output);
    }

    /**
     * See what the AI tool returns for location connections.
     */
    @GET
    @Path("/ai/location-connections/{id}")
    public TemplateInstance aiLocationConnections(@RestPath String id) {
        String output = campaignTools.getLocationConnections(id);
        return Templates.aiToolOutput("Location Connections", output);
    }

    /**
     * See what the AI tool returns for shared history.
     */
    @GET
    @Path("/ai/shared-history")
    public TemplateInstance aiSharedHistory(@RestQuery String char1Id, @RestQuery String char2Id) {
        if (char1Id == null || char2Id == null) {
            badRequest();
            return null;
        }
        String output = campaignTools.getSharedHistory(char1Id, char2Id);
        return Templates.aiToolOutput("Shared History", output);
    }
}
