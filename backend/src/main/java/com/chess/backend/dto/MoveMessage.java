package com.chess.backend.dto;

/**
 * Wire shape of the client -> server WebSocket message that requests a move:
 * {"type":"MOVE","gameId":"ABC123","from":"e2","to":"e4"}.
 */
public record MoveMessage(String type, String gameId, String from, String to) {
}
