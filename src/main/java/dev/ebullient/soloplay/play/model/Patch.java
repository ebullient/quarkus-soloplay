package dev.ebullient.soloplay.play.model;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public record Patch(
        @Description("'location' or 'actor' (for NPC or creature)") String type,
        @Description("Name") String name,
        @Description("short, stable, identifying summary") String summary,
        @Description("Longer, story-informed description of location or NPC") String description,
        @Description("Tags for additional information: plot threads, alignment, faction, affiliation, etc.") List<String> tags,
        @Description("Alternate names this location or NPC would be known by. For example, Commodore Krux may have 'Krux' and 'The Commodore' as aliases") List<String> aliases,
        @Description("list of lore document filenames used. If you did not use lore docs or tools, sources = []. Don't invent filenames.") List<String> sources) {
}
