package com.chess.backend.websocket;

import com.chess.backend.model.Game;
import com.chess.backend.repository.InMemoryGameRepository;
import com.chess.backend.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * These are the checks that stop a WebSocket connection from ever reaching
 * the handler in the first place — a game that doesn't exist, or a player
 * who isn't actually seated in it (i.e. trying to connect as a color that
 * isn't theirs), never gets past the handshake.
 */
class GameHandshakeInterceptorTest {

    private final GameService gameService = new GameService(new InMemoryGameRepository());
    private final GameHandshakeInterceptor interceptor = new GameHandshakeInterceptor(gameService);

    @Test
    void acceptsAConnectionForASeatedPlayerAndRecordsItsIdentity() throws Exception {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                request(game.getGameId(), "Alice"), mock(ServerHttpResponse.class), null, attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes)
                .containsEntry(GameHandshakeInterceptor.GAME_ID_ATTRIBUTE, game.getGameId())
                .containsEntry(GameHandshakeInterceptor.PLAYER_ATTRIBUTE, "Alice");
    }

    @Test
    void rejectsAConnectionForAGameThatDoesNotExist() throws Exception {
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        boolean accepted = interceptor.beforeHandshake(
                request("NOPE00", "Alice"), response, null, new HashMap<>());

        assertThat(accepted).isFalse();
        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsAConnectionForAPlayerNotSeatedInTheGame() throws Exception {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        // "Mallory" isn't White or Black in this game — this is what stops
        // someone from connecting and acting as a color that isn't theirs.
        boolean accepted = interceptor.beforeHandshake(
                request(game.getGameId(), "Mallory"), response, null, new HashMap<>());

        assertThat(accepted).isFalse();
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectsAConnectionMissingGameIdOrPlayer() throws Exception {
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        boolean accepted = interceptor.beforeHandshake(
                request(null, "Alice"), response, null, new HashMap<>());

        assertThat(accepted).isFalse();
        verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
    }

    private static ServerHttpRequest request(String gameId, String player) {
        StringBuilder query = new StringBuilder();
        if (gameId != null) {
            query.append("gameId=").append(gameId);
        }
        if (player != null) {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append("player=").append(player);
        }

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("http://localhost/ws/game?" + query));
        return request;
    }
}
