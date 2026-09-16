package com.chess.backend.websocket;

import com.chess.backend.dto.ErrorMessage;
import com.chess.backend.dto.GameStateMessage;
import com.chess.backend.dto.MoveMessage;
import com.chess.backend.dto.PlayerStatusMessage;
import com.chess.backend.model.Game;
import com.chess.backend.model.GameStatus;
import com.chess.backend.model.PlayerColor;
import com.chess.backend.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges the WebSocket transport and {@link GameService}. This handler is a
 * singleton shared by every connection, so all per-game session bookkeeping
 * lives in {@code sessionsByGameId} rather than per-connection state. It has
 * no chess knowledge of its own: it only decodes MOVE messages, hands them to
 * GameService, and encodes whatever GameService decides back onto the wire.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameService gameService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Set<WebSocketSession>> sessionsByGameId = new ConcurrentHashMap<>();

    public GameWebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String gameId = gameIdOf(session);
        String player = playerOf(session);
        boolean wasAlreadyConnected = hasSessionForPlayer(gameId, player);
        sessionsByGameId.computeIfAbsent(gameId, id -> ConcurrentHashMap.newKeySet()).add(session);

        gameService.findGame(gameId).ifPresent(game -> {
            sendQuietly(session, GameStateMessage.from(game));
            if (!wasAlreadyConnected && game.getStatus() == GameStatus.IN_PROGRESS) {
                PlayerColor color = colorOf(game, player);
                if (color != null) {
                    broadcast(gameId, new PlayerStatusMessage(gameId, color, true));
                }
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        MoveMessage move;
        try {
            move = objectMapper.readValue(message.getPayload(), MoveMessage.class);
        } catch (Exception e) {
            sendQuietly(session, new ErrorMessage("Malformed message"));
            return;
        }

        if (!"MOVE".equals(move.type())) {
            sendQuietly(session, new ErrorMessage("Unsupported message type: " + move.type()));
            return;
        }

        String sessionGameId = gameIdOf(session);
        if (!sessionGameId.equals(move.gameId())) {
            sendQuietly(session, new ErrorMessage("Move is for a different game than this connection"));
            return;
        }

        try {
            Game updated = gameService.makeMove(sessionGameId, playerOf(session), move.from(), move.to());
            broadcast(sessionGameId, GameStateMessage.from(updated));
        } catch (ResponseStatusException e) {
            sendQuietly(session, new ErrorMessage(e.getReason()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String gameId = gameIdOf(session);
        String player = playerOf(session);
        Set<WebSocketSession> sessions = sessionsByGameId.get(gameId);
        if (sessions != null) {
            sessions.remove(session);
        }

        if (!hasSessionForPlayer(gameId, player)) {
            gameService.findGame(gameId).ifPresent(game -> {
                if (game.getStatus() == GameStatus.IN_PROGRESS) {
                    PlayerColor color = colorOf(game, player);
                    if (color != null) {
                        broadcast(gameId, new PlayerStatusMessage(gameId, color, false));
                    }
                }
            });
        }

        // Once literally nobody is left connected, reap the game so it
        // doesn't permanently occupy the single game slot — see
        // GameService.abandonIfUnwatched. A player still watching (the
        // common disconnect case handled above) keeps the game alive.
        if (sessions == null || sessions.isEmpty()) {
            gameService.abandonIfUnwatched(gameId);
        }
    }

    /**
     * Pushes the current game state to every connected session for that
     * game. Used by {@code GameController} after a REST call — like a
     * player joining — changes the game outside of a WebSocket message, so
     * an already-connected player (e.g. the creator, waiting for an
     * opponent) isn't left showing a stale snapshot from their initial
     * connect.
     */
    public void broadcastGameState(Game game) {
        broadcast(game.getGameId(), GameStateMessage.from(game));
    }

    private void broadcast(String gameId, Object message) {
        Set<WebSocketSession> sessions = sessionsByGameId.get(gameId);
        if (sessions == null) {
            return;
        }
        for (WebSocketSession session : sessions) {
            sendQuietly(session, message);
        }
    }

    private boolean hasSessionForPlayer(String gameId, String player) {
        Set<WebSocketSession> sessions = sessionsByGameId.get(gameId);
        if (sessions == null) {
            return false;
        }
        for (WebSocketSession session : sessions) {
            if (player.equals(playerOf(session))) {
                return true;
            }
        }
        return false;
    }

    private static PlayerColor colorOf(Game game, String player) {
        if (player.equals(game.getWhitePlayer())) {
            return PlayerColor.WHITE;
        }
        if (player.equals(game.getBlackPlayer())) {
            return PlayerColor.BLACK;
        }
        return null;
    }

    private void sendQuietly(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            // Best-effort: a failed send just means this one player misses
            // this update. afterConnectionClosed reclaims the session once
            // the transport notices it's gone.
        }
    }

    private static String gameIdOf(WebSocketSession session) {
        return (String) session.getAttributes().get(GameHandshakeInterceptor.GAME_ID_ATTRIBUTE);
    }

    private static String playerOf(WebSocketSession session) {
        return (String) session.getAttributes().get(GameHandshakeInterceptor.PLAYER_ATTRIBUTE);
    }
}
