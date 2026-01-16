package dev.ebullient.soloplay.play;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.ebullient.soloplay.GameRepository;

@ApplicationScoped
public class GameEngine {

    @Inject
    GameRepository gameRepository;
}
