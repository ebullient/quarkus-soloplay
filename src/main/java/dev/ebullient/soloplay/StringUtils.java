package dev.ebullient.soloplay;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension(namespace = "util")
public class StringUtils {
    /**
     * Convert a name into a URL-friendly slug.
     */
    public static String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "untitled";
        }
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .trim()
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }

    public static String valueOrPlaceholder(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    public static String valueOrPlaceholder(Integer value) {
        return value == null ? "—" : value.toString();
    }

    public static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    public static String valueOrPlaceholder(Collection<String> list) {
        if (list == null || list.isEmpty()) {
            return "—";
        }
        return String.join(", ", list);
    }

    public static String normalize(String value) {
        return value.trim().toLowerCase();
    }

    public static Collection<String> normalize(Collection<String> value) {
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .map(s -> normalize(s))
                .filter(s -> !s.isBlank())
                .toList();
    }

    public static String formatEpoch(Long epochMillis) {
        if (epochMillis == null) {
            return "—";
        }
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
