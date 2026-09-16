package com.chess.backend.dto;

/**
 * Sent only to the WebSocket session whose own message was rejected — never
 * broadcast to the other player.
 */
public record ErrorMessage(String type, String message) {

    public ErrorMessage(String message) {
        this("ERROR", message);
    }
}
