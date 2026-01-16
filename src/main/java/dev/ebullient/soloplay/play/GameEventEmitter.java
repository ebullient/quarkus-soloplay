package dev.ebullient.soloplay.play;

/**
 * Per-request event sink for emitting incremental updates (e.g. status text)
 * while an engine is processing a request.
 */
@FunctionalInterface
public interface GameEventEmitter {
    void assistantDelta(String text);

    static GameEventEmitter noop() {
        return text -> {
        };
    }
}
