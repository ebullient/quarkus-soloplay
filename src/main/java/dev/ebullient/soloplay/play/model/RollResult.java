package dev.ebullient.soloplay.play.model;

public record RollResult(
        String type, // matches pendingRoll.type
        int total, // final result after modifiers
        String breakdown, // "14 + 3 = 17"
        boolean success, // did it meet/beat DC?
        String context) { // copied from pendingRoll for continuity
}
