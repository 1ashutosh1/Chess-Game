package com.chess.backend.model;

public enum GameStatus {
    WAITING,
    IN_PROGRESS,
    // Checkmate, draw, resignation, or abandonment — the specific reason
    // is derived from the game's FEN/history when needed, not stored here.
    COMPLETED
}
