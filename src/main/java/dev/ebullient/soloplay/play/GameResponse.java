package dev.ebullient.soloplay.play;

import java.util.List;

public sealed interface GameResponse {

    static Reply reply(String assistantMarkdown, GameEffect... effects) {
        return new Reply(assistantMarkdown, effects == null ? List.of() : List.of(effects));
    }

    static Error error(String message) {
        return new Error(message);
    }

    record Reply(String assistantMarkdown, List<GameEffect> effects) implements GameResponse {
        public Reply {
            if (effects == null) {
                effects = List.of();
            }
        }
    }

    record Error(String message) implements GameResponse {
    }
}
