package com.chess.backend.dto;

import com.chess.backend.model.Game;
import com.chess.backend.model.GameStatus;
import com.chess.backend.model.PlayerColor;

/**
 * The authoritative game state broadcast over WebSocket to every player in a
 * game after a successful move, and once on connect so a rejoining player is
 * brought up to date without waiting for the next move.
 */
public record GameStateMessage(
        String type,
        String gameId,
        String fen,
        PlayerColor turn,
        GameStatus status) {

    public static GameStateMessage from(Game game) {
        return new GameStateMessage("GAME_STATE", game.getGameId(), game.getFen(), game.getTurn(), game.getStatus());
    }
}
