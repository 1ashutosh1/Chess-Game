package com.chess.backend.controller;

import com.chess.backend.dto.GameResponse;
import com.chess.backend.dto.PlayerRequest;
import com.chess.backend.model.Game;
import com.chess.backend.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<GameResponse> createGame(@RequestBody(required = false) PlayerRequest request) {
        String playerName = request != null ? request.playerName() : null;
        Game game = gameService.createGame(playerName);
        return ResponseEntity.created(URI.create("/api/games/" + game.getGameId()))
                .body(GameResponse.from(game));
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<GameResponse> joinGame(
            @PathVariable String gameId,
            @RequestBody(required = false) PlayerRequest request) {
        String playerName = request != null ? request.playerName() : null;
        Game game = gameService.joinGame(gameId, playerName);
        return ResponseEntity.ok(GameResponse.from(game));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable String gameId) {
        Game game = gameService.getGame(gameId);
        return ResponseEntity.ok(GameResponse.from(game));
    }
}
