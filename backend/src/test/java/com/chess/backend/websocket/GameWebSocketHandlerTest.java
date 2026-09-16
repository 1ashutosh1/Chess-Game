package com.chess.backend.websocket;

import com.chess.backend.model.Game;
import com.chess.backend.repository.InMemoryGameRepository;
import com.chess.backend.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameWebSocketHandlerTest {

    private final GameService gameService = new GameService(new InMemoryGameRepository());
    private final GameWebSocketHandler handler = new GameWebSocketHandler(gameService);

    private String gameId;
    private WebSocketSession aliceSession;
    private WebSocketSession bobSession;

    @BeforeEach
    void setUpGameAndSessions() throws Exception {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");
        gameId = game.getGameId();

        aliceSession = fakeSession(gameId, "Alice");
        bobSession = fakeSession(gameId, "Bob");

        handler.afterConnectionEstablished(aliceSession);
        handler.afterConnectionEstablished(bobSession);
        // Ignore the initial GAME_STATE snapshot each connect sends, so
        // each test only observes the messages it triggers itself.
        clearInvocations(aliceSession, bobSession);
    }

    @Test
    void legalMoveIsBroadcastToBothPlayers() throws Exception {
        handler.handleTextMessage(aliceSession, move("e2", "e4"));

        verify(aliceSession).sendMessage(any(TextMessage.class));
        verify(bobSession).sendMessage(any(TextMessage.class));
        assertThat(lastPayloadSent(aliceSession)).contains("\"type\":\"GAME_STATE\"", "\"turn\":\"BLACK\"");
        assertThat(lastPayloadSent(bobSession)).contains("\"type\":\"GAME_STATE\"", "\"turn\":\"BLACK\"");
    }

    @Test
    void illegalMoveSendsErrorOnlyToTheSender() throws Exception {
        // A pawn cannot jump three squares.
        handler.handleTextMessage(aliceSession, move("e2", "e5"));

        verify(aliceSession).sendMessage(any(TextMessage.class));
        verify(bobSession, never()).sendMessage(any(TextMessage.class));
        assertThat(lastPayloadSent(aliceSession)).contains("\"type\":\"ERROR\"");
    }

    @Test
    void wrongTurnSendsErrorOnlyToTheSender() throws Exception {
        // It's White's move first; Bob (Black) tries to move anyway.
        handler.handleTextMessage(bobSession, move("e7", "e5"));

        verify(bobSession).sendMessage(any(TextMessage.class));
        verify(aliceSession, never()).sendMessage(any(TextMessage.class));
        assertThat(lastPayloadSent(bobSession)).contains("\"type\":\"ERROR\"");
    }

    @Test
    void disconnectRemovesTheSessionSoLaterBroadcastsSkipIt() throws Exception {
        handler.handleTextMessage(aliceSession, move("e2", "e4")); // now it's Black's turn
        clearInvocations(aliceSession, bobSession);

        handler.afterConnectionClosed(aliceSession, CloseStatus.NORMAL);
        handler.handleTextMessage(bobSession, move("e7", "e5"));

        verify(bobSession).sendMessage(any(TextMessage.class));
        verify(aliceSession, never()).sendMessage(any(TextMessage.class));
    }

    private TextMessage move(String from, String to) {
        return new TextMessage(
                "{\"type\":\"MOVE\",\"gameId\":\"" + gameId + "\",\"from\":\"" + from + "\",\"to\":\"" + to + "\"}");
    }

    private static WebSocketSession fakeSession(String gameId, String player) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(GameHandshakeInterceptor.GAME_ID_ATTRIBUTE, gameId);
        attributes.put(GameHandshakeInterceptor.PLAYER_ATTRIBUTE, player);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static String lastPayloadSent(WebSocketSession session) throws Exception {
        var captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(captor.capture());
        return captor.getValue().getPayload();
    }
}
