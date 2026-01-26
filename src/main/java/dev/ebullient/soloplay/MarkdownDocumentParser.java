package dev.ebullient.soloplay;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import io.quarkus.logging.Log;

@ApplicationScoped
public class MarkdownDocumentParser {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    // Regex pattern for YAML frontmatter
    // (?s) enables DOTALL mode (. matches newlines)
    // ^--- matches opening ---
    // (.*?) captures content (non-greedy)
    // \n--- matches closing ---
    static final java.util.regex.Pattern YAML_FRONTMATTER_PATTERN = java.util.regex.Pattern
            .compile("(?s)^---\\s*\\n(.*?)\\n---\\s*\\n");

    // Block reference pattern: ^blockid at end of line
    static final java.util.regex.Pattern BLOCK_REF_PATTERN = java.util.regex.Pattern
            .compile("\\s*\\^[a-zA-Z0-9-]+$", java.util.regex.Pattern.MULTILINE);

    // Inline tag pattern: #tag (not inside code or links)
    static final java.util.regex.Pattern TAG_PATTERN = java.util.regex.Pattern
            .compile("(?<![`\\[])#([a-zA-Z][a-zA-Z0-9_/-]*)");

    // Multiple blank lines
    static final java.util.regex.Pattern MULTIPLE_BLANK_LINES = java.util.regex.Pattern
            .compile("\\n{3,}");

    public Document parse(String filename, String content) {
        Map<String, Object> frontmatter = parseFrontmatter(content);
        frontmatter.put("sourceFile", filename);
        frontmatter.put("canonical", "true");

        String body = removeFrontmatter(content);
        String prefix = "";
        if (frontmatter.containsKey("adventureName")) {
            prefix += "Adventure: %s\n\n".formatted(frontmatter.get("adventureName"));
        }
        if (frontmatter.containsKey("chapterName")) {
            prefix += "Chapter %s: %s\n\n".formatted(frontmatter.get("chapterNumber"), frontmatter.get("chapterName"));
        }
        frontmatter.put("groupPrefix", prefix);

        String label = deriveLabelFromFilename(filename);
        frontmatter.put("label", label);

        Metadata metadata = Metadata.from(frontmatter);

        // Extract inline tags before cleaning
        String tags = extractTags(body, metadata.getString("tags"));
        if (!tags.isEmpty()) {
            metadata.put("tags", tags);
        }

        // Clean up body
        body = cleanBody(body);
        return Document.from(body, metadata);
    }

    String removeFrontmatter(String content) {
        return YAML_FRONTMATTER_PATTERN.matcher(content).replaceFirst("").trim();
    }

    Map<String, Object> parseFrontmatter(String content) {
        var matcher = YAML_FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return Map.of();
        }
        try {
            String yaml = matcher.group(1).trim();

            @SuppressWarnings("unchecked")
            Map<String, Object> raw = YAML_MAPPER.readValue(yaml, Map.class);

            // Use first alias as name if name not present
            if (!raw.containsKey("name") && raw.containsKey("aliases")) {
                Object aliases = raw.get("aliases");
                if (aliases instanceof List<?> list && !list.isEmpty()) {
                    raw.put("name", list.get(0));
                }
            }

            // Flatten lists to comma-separated strings
            Map<String, Object> result = new HashMap<>();
            Object loreTags = raw.get("loreTags");
            if (loreTags instanceof List<?> loreTagList) {
                parseHierarchicalTags(loreTagList, result);
            }

            raw.forEach((key, value) -> {
                if (value == null)
                    return;
                if (value instanceof List<?> list) {
                    result.put(key, list.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(",")));
                } else {
                    result.put(key, value.toString());
                }
            });
            return result;
        } catch (Exception e) {
            Log.warnf("Failed to parse frontmatter: %s", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Extract inline #tags from body content.
     */
    String extractTags(String body, String existingTags) {
        var tags = new HashSet<String>();
        if (existingTags != null && !existingTags.isBlank()) {
            tags.addAll(Arrays.asList(existingTags.split(",")));
        }
        var matcher = TAG_PATTERN.matcher(body);
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
        return String.join(",", tags);
    }

    /**
     * Parse hierarchical loreTags into structured metadata.
     * Example: "lore/monster/cr/8" → metadata.put("monster.cr", "8")
     *
     * @param loreTags List of hierarchical tags from YAML
     * @param metadata Metadata map to populate
     */
    private void parseHierarchicalTags(List<?> loreTags, Map<String, Object> metadata) {
        if (loreTags == null || loreTags.isEmpty()) {
            return;
        }

        boolean contentTypeSet = false;

        for (Object tag : loreTags) {
            String tagStr = tag.toString();

            // Only process tags with "lore/" prefix
            if (!tagStr.startsWith("lore/")) {
                continue;
            }

            // Remove "lore/" prefix
            String path = tagStr.substring(5); // "monster/cr/8"

            // Split into parts: ["monster", "cr", "8"]
            String[] parts = path.split("/");

            if (parts.length == 0 || "compendium".equals(parts[0])) {
                continue;
            }

            // Set contentType from first lore/ tag encountered
            if (!contentTypeSet) {
                metadata.put("contentType", parts[0]);
                contentTypeSet = true;
            }

            final String key;
            final String value;
            // Parse nested properties
            if (parts.length == 2) {
                key = parts[0];
                value = parts[1];
            } else if (parts.length >= 3) {
                // Complex case: "lore/monster/cr/8" → metadata["monster.cr"] = "8"
                // Build dotted key from all parts except the last
                StringBuilder keyBuilder = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length - 1; i++) {
                    keyBuilder.append(".").append(parts[i]);
                }
                key = keyBuilder.toString();
                value = parts[parts.length - 1].replaceAll("\\s+", " ").trim();
            } else {
                // Simple case: "lore/statblock" → contentType already set, no other value to
                // save
                continue;
            }

            if (value.isEmpty()) {
                continue; // Skip tags with empty values
            }

            // Convert to list if multiple of the same key, e.g.
            // - lore/monster/environment/grassland
            // - lore/monster/environment/hill
            // - lore/monster/environment/mountain
            metadata.merge(key, value, (v1, v2) -> v1 + ", " + v2);
        }
    }

    /**
     * Clean up body content:
     * - Remove block references (^blockid)
     * - Normalize whitespace (collapse multiple blank lines)
     */
    String cleanBody(String body) {
        // Remove block references
        body = BLOCK_REF_PATTERN.matcher(body).replaceAll("");
        // Collapse multiple blank lines to single
        body = MULTIPLE_BLANK_LINES.matcher(body).replaceAll("\n\n");
        return body.trim();
    }

    /**
     * Derive a PascalCase singular label from a filename.
     * Example: "items.txt" → "Item", "magic-items.txt" → "MagicItem"
     */
    private String deriveLabelFromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        // Remove extension
        String baseName = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;

        if (baseName.isBlank()) {
            return null;
        }

        // Singularization rules for common English plural forms
        if (baseName.endsWith("ies")) {
            // abilities → ability
            baseName = baseName.substring(0, baseName.length() - 3) + "y";
        } else if (baseName.endsWith("sses")) {
            // classes → class
            baseName = baseName.substring(0, baseName.length() - 2);
        } else if (baseName.endsWith("xes")) {
            // boxes → box
            baseName = baseName.substring(0, baseName.length() - 2);
        } else if (baseName.endsWith("ches")) {
            // watches → watch
            baseName = baseName.substring(0, baseName.length() - 2);
        } else if (baseName.endsWith("shes")) {
            // dishes → dish
            baseName = baseName.substring(0, baseName.length() - 2);
        } else if (baseName.endsWith("s") && !baseName.endsWith("ss")) {
            // items → item, adventures → adventure, monsters → monster
            baseName = baseName.substring(0, baseName.length() - 1);
        }

        // Convert to PascalCase, handling hyphens and underscores
        // "magic-item" → "MagicItem", "magic_item" → "MagicItem"
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : baseName.toCharArray()) {
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
