package com.chess.backend.websocket;

import com.chess.backend.model.Game;
import com.chess.backend.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Identifies the game and player for a connection from its query string
 * (e.g. /ws/game?gameId=ABC123&player=Alice) before the handshake completes,
 * so the handler only ever deals with sessions that are already known-good.
 * Connections for a missing game or an unseated player are rejected here
 * instead of being accepted and failing on the first message.
 */
@Component
public class GameHandshakeInterceptor implements HandshakeInterceptor {

    public static final String GAME_ID_ATTRIBUTE = "gameId";
    public static final String PLAYER_ATTRIBUTE = "player";

    private final GameService gameService;

    public GameHandshakeInterceptor(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {

        Map<String, List<String>> query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String gameId = firstValue(query, "gameId");
        String player = firstValue(query, "player");

        if (gameId == null || player == null) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        Optional<Game> game = gameService.findGame(gameId);
        if (game.isEmpty()) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        }

        if (!player.equals(game.get().getWhitePlayer()) && !player.equals(game.get().getBlackPlayer())) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put(GAME_ID_ATTRIBUTE, gameId);
        attributes.put(PLAYER_ATTRIBUTE, player);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do — session attributes were already populated above.
    }

    private static String firstValue(Map<String, List<String>> query, String key) {
        List<String> values = query.get(key);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
