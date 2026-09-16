package com.chess.backend.service;

import com.chess.backend.model.Game;
import com.chess.backend.model.GameStatus;
import com.chess.backend.model.PlayerColor;
import com.chess.backend.repository.InMemoryGameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {

    private final GameService gameService = new GameService(new InMemoryGameRepository());

    @Test
    void createGameStartsWhiteToMoveOnTheStandardPosition() {
        Game game = gameService.createGame("Alice");

        assertThat(game.getWhitePlayer()).isEqualTo("Alice");
        assertThat(game.getBlackPlayer()).isNull();
        assertThat(game.getTurn()).isEqualTo(PlayerColor.WHITE);
        assertThat(game.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(game.getFen()).isEqualTo("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        assertThat(game.getGameId()).hasSize(6);
    }

    @Test
    void createGameDefaultsPlayerNameWhenNotGiven() {
        Game game = gameService.createGame(null);

        assertThat(game.getWhitePlayer()).isEqualTo("White");
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

    @Test
    void getGameThrowsWhenGameDoesNotExist() {
        assertThatThrownBy(() -> gameService.getGame("NOPE00"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void joinGameSeatsSecondPlayerAsBlackAndStartsTheGame() {
        Game created = gameService.createGame("Alice");

        Game joined = gameService.joinGame(created.getGameId(), "Bob");

        assertThat(joined.getBlackPlayer()).isEqualTo("Bob");
        assertThat(joined.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(joined.getWhitePlayer()).isEqualTo("Alice");
    }

    @Test
    void joinGameDefaultsPlayerNameWhenNotGiven() {
        Game created = gameService.createGame("Alice");

        Game joined = gameService.joinGame(created.getGameId(), null);

        assertThat(joined.getBlackPlayer()).isEqualTo("Black");
    }

    @Test
    void joinGameThrowsWhenGameDoesNotExist() {
        assertThatThrownBy(() -> gameService.joinGame("NOPE00", "Bob"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void joinGameThrowsWhenGameAlreadyHasABlackPlayer() {
        Game created = gameService.createGame("Alice");
        gameService.joinGame(created.getGameId(), "Bob");

        assertThatThrownBy(() -> gameService.joinGame(created.getGameId(), "Charlie"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
