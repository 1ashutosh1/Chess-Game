package com.chess.backend.dto;

import com.chess.backend.model.Game;
import com.chess.backend.model.GameStatus;
import com.chess.backend.model.PlayerColor;

/**
 * Wire representation of a Game, returned by every /api/games endpoint.
 * Kept separate from the Game model so the API shape can evolve
 * independently of the domain object.
 */
public record GameResponse(
        String gameId,
        String whitePlayer,
        String blackPlayer,
        String fen,
        PlayerColor turn,
        GameStatus status) {

    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getGameId(),
                game.getWhitePlayer(),
                game.getBlackPlayer(),
                game.getFen(),
                game.getTurn(),
                game.getStatus());
    }
}
