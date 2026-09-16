package com.chess.backend.repository;

import com.chess.backend.model.Game;

import java.util.Optional;

/**
 * Storage boundary for games. GameService depends only on this interface,
 * so the backing store (in-memory today, PostgreSQL/JPA later) can change
 * without touching business logic.
 */
public interface GameRepository {

    Game save(Game game);

    Optional<Game> findById(String gameId);

    /**
     * The single game this MVP currently has room for, regardless of its id
     * — used only to check whether a new game can be created yet.
     */
    Optional<Game> findCurrent();
}
