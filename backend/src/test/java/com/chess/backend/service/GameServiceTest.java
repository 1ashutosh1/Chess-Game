package com.chess.backend.service;

import com.chess.backend.model.Game;
import com.chess.backend.model.GameStatus;
import com.chess.backend.model.PlayerColor;
import com.chess.backend.repository.InMemoryGameRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceTest {

    private final GameService gameService = new GameService(new InMemoryGameRepository());

    @Test
    void createGameStartsWhiteToMoveOnTheStandardPosition() {
        Game game = gameService.createGame("Alice");

        assertThat(game.getWhitePlayer()).isEqualTo("Alice");
        assertThat(game.getBlackPlayer()).isNull();
        assertThat(game.getTurn()).isEqualTo(PlayerColor.WHITE);
        assertThat(game.getStatus()).isEqualTo(GameStatus.WAITING_FOR_OPPONENT);
        assertThat(game.getFen()).isEqualTo("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        assertThat(game.getGameId()).hasSize(6);
    }

    @Test
    void createdGameCanBeFoundById() {
        Game created = gameService.createGame("Alice");

        Optional<Game> found = gameService.findGame(created.getGameId());

        assertThat(found).contains(created);
    }

    @Test
    void unknownGameIdIsNotFound() {
        assertThat(gameService.findGame("NOPE00")).isEmpty();
    }
}
