package com.chess.backend.config;

import com.chess.backend.websocket.GameHandshakeInterceptor;
import com.chess.backend.websocket.GameWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the native WebSocket endpoint players connect to for live moves.
 * No STOMP, no message broker — one handler, one path, plain JSON frames.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    private final GameWebSocketHandler gameWebSocketHandler;
    private final GameHandshakeInterceptor gameHandshakeInterceptor;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler, GameHandshakeInterceptor gameHandshakeInterceptor) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.gameHandshakeInterceptor = gameHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game")
                .addInterceptors(gameHandshakeInterceptor)
                .setAllowedOrigins(allowedOrigin);
    }
}
