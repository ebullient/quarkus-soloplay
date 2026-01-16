package dev.ebullient.soloplay.play;

import dev.ebullient.soloplay.play.model.Draft;

public sealed interface GameEffect {

    record DraftUpdate(String key, Draft draft) implements GameEffect {
    }

    /**
     * Pre-rendered HTML fragment intended for a specific UI slot.
     * Prefer templated server HTML (Qute) over arbitrary model output.
     */
    record HtmlFragment(String slot, String html) implements GameEffect {
    }

    /**
     * Arbitrary JSON payload for client-side rendering.
     */
    record JsonPayload(String name, Object payload) implements GameEffect {
    }
}
