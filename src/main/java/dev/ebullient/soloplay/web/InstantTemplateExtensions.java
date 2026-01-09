package dev.ebullient.soloplay.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import io.quarkus.qute.TemplateExtension;

/**
 * Qute template extensions for formatting Instant objects.
 * Instant objects don't support date/time patterns directly, so we convert to ZonedDateTime first.
 */
@TemplateExtension
public class InstantTemplateExtensions {

    /**
     * Format an Instant using a pattern string in UTC timezone.
     *
     * @param instant the instant to format
     * @param pattern the datetime pattern (e.g., "yyyy-MM-dd HH:mm", "MMM dd, yyyy")
     * @return formatted datetime string
     */
    public static String formatUtc(Instant instant, String pattern) {
        if (instant == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.of("UTC"));
        return formatter.format(instant);
    }
}
