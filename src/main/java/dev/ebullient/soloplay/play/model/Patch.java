package dev.ebullient.soloplay.play.model;

import java.util.List;

public record Patch(
        String type,
        String name,
        String summary,
        String description,
        List<String> tags,
        List<String> aliases,
        List<String> sources) {
}
