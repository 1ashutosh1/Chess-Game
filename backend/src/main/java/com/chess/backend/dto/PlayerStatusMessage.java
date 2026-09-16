package com.chess.backend.dto;

import com.chess.backend.model.PlayerColor;

/**
 * Broadcast whenever a seat's connection to the game transitions, so the
 * other player is never left staring at a board that will never move again
 * with no explanation. {@code connected=false} means that seat just lost its
 * last open WebSocket session; {@code connected=true} means it just gained
 * its first one (e.g. reconnecting after a refresh).
 */
public record PlayerStatusMessage(String type, String gameId, PlayerColor player, boolean connected) {

    public PlayerStatusMessage(String gameId, PlayerColor player, boolean connected) {
        this("PLAYER_STATUS", gameId, player, connected);
    }
}
