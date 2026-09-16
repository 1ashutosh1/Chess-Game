package com.chess.backend.dto;

/**
 * Body for both POST /api/games and POST /api/games/{gameId}/join.
 * playerName is optional — there is no authentication yet, so an omitted
 * name simply falls back to a default ("White" or "Black").
 */
public record PlayerRequest(String playerName) {
}
