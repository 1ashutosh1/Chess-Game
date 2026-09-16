package com.chess.backend.repository;

import com.chess.backend.model.Game;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the single active game in memory. AtomicReference gives thread-safe
 * get/set of that one slot — the same guarantee ConcurrentHashMap would give
 * per-key, without a map's per-key indexing, which this MVP has no use for
 * since only one game exists at a time. Creating a new game simply replaces
 * the reference, so there is nothing to expire or clean up.
 */
@Repository
public class InMemoryGameRepository implements GameRepository {

    private final AtomicReference<Game> currentGame = new AtomicReference<>();

    @Override
    public Game save(Game game) {
        currentGame.set(game);
        return game;
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return Optional.ofNullable(currentGame.get())
                .filter(game -> game.getGameId().equals(gameId));
    }

    @Override
    public Optional<Game> findCurrent() {
        return Optional.ofNullable(currentGame.get());
    }
}
