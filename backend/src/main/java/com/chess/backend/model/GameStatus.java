package com.chess.backend.model;

public enum GameStatus {
    // Created by the first player, waiting for the second to join.
    WAITING,
    // Both players present, moves can be made.
    IN_PROGRESS,
    // Checkmate, draw, resignation, or abandonment — the specific reason
    // is derived from the game's FEN/history when needed, not stored here.
    COMPLETED
}
