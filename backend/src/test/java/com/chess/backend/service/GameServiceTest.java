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
    void createGameThrowsWhenAGameIsAlreadyWaitingForAnOpponent() {
        gameService.createGame("Alice");

        assertThatThrownBy(() -> gameService.createGame("Someone Else"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createGameThrowsWhenAGameIsAlreadyInProgress() {
        Game created = gameService.createGame("Alice");
        gameService.joinGame(created.getGameId(), "Bob");

        assertThatThrownBy(() -> gameService.createGame("Someone Else"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createGameSucceedsAgainOnceThePreviousGameHasFinished() {
        Game created = gameService.createGame("Alice");
        gameService.joinGame(created.getGameId(), "Bob");
        // Fool's mate: fastest possible checkmate, so the game is COMPLETED.
        gameService.makeMove(created.getGameId(), "Alice", "f2", "f3");
        gameService.makeMove(created.getGameId(), "Bob", "e7", "e5");
        gameService.makeMove(created.getGameId(), "Alice", "g2", "g4");
        gameService.makeMove(created.getGameId(), "Bob", "d8", "h4");

        Game next = gameService.createGame("Someone Else");

        assertThat(next.getGameId()).isNotEqualTo(created.getGameId());
        assertThat(next.getStatus()).isEqualTo(GameStatus.WAITING);
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

    @Test
    void makeMoveAppliesALegalMoveAndFlipsTheTurn() {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");

        Game moved = gameService.makeMove(game.getGameId(), "Alice", "e2", "e4");

        assertThat(moved.getFen())
                .isEqualTo("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        assertThat(moved.getTurn()).isEqualTo(PlayerColor.BLACK);
        assertThat(moved.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void makeMoveThrowsWhenTheMoveIsNotLegal() {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");

        assertThatThrownBy(() -> gameService.makeMove(game.getGameId(), "Alice", "e2", "e5"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void makeMoveThrowsWhenThePlayerIsNotSeatedInTheGame() {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");

        // Neither Alice nor Bob — this player has no seat, let alone a color,
        // in this game.
        assertThatThrownBy(() -> gameService.makeMove(game.getGameId(), "Mallory", "e2", "e4"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void makeMoveThrowsWhenItIsNotThatPlayersTurn() {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");

        assertThatThrownBy(() -> gameService.makeMove(game.getGameId(), "Bob", "e7", "e5"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void makeMoveDetectsCheckmateAndCompletesTheGame() {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");

        // Fool's mate: fastest possible checkmate.
        gameService.makeMove(game.getGameId(), "Alice", "f2", "f3");
        gameService.makeMove(game.getGameId(), "Bob", "e7", "e5");
        gameService.makeMove(game.getGameId(), "Alice", "g2", "g4");
        Game mated = gameService.makeMove(game.getGameId(), "Bob", "d8", "h4");

        assertThat(mated.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(mated.getTurn()).isEqualTo(PlayerColor.WHITE);
    }

    @Test
    void makeMoveThrowsWhenTheGameIsAlreadyFinished() {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");
        gameService.makeMove(game.getGameId(), "Alice", "f2", "f3");
        gameService.makeMove(game.getGameId(), "Bob", "e7", "e5");
        gameService.makeMove(game.getGameId(), "Alice", "g2", "g4");
        gameService.makeMove(game.getGameId(), "Bob", "d8", "h4");

        assertThatThrownBy(() -> gameService.makeMove(game.getGameId(), "Alice", "a2", "a3"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void abandonIfUnwatchedCompletesAnInProgressGameAndFreesTheSlot() {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");

        gameService.abandonIfUnwatched(game.getGameId());

        assertThat(gameService.getGame(game.getGameId()).getStatus()).isEqualTo(GameStatus.COMPLETED);
        // The slot is freed immediately — no CONFLICT this time.
        Game next = gameService.createGame("Someone Else");
        assertThat(next.getGameId()).isNotEqualTo(game.getGameId());
    }

    @Test
    void abandonIfUnwatchedCompletesAWaitingGame() {
        Game game = gameService.createGame("Alice");

        gameService.abandonIfUnwatched(game.getGameId());

        assertThat(gameService.getGame(game.getGameId()).getStatus()).isEqualTo(GameStatus.COMPLETED);
    }

    @Test
    void abandonIfUnwatchedIsANoopForAGameThatAlreadyFinishedNormally() {
        Game game = gameService.joinGame(gameService.createGame("Alice").getGameId(), "Bob");
        gameService.makeMove(game.getGameId(), "Alice", "f2", "f3");
        gameService.makeMove(game.getGameId(), "Bob", "e7", "e5");
        gameService.makeMove(game.getGameId(), "Alice", "g2", "g4");
        gameService.makeMove(game.getGameId(), "Bob", "d8", "h4"); // Fool's mate

        gameService.abandonIfUnwatched(game.getGameId());

        assertThat(gameService.getGame(game.getGameId()).getStatus()).isEqualTo(GameStatus.COMPLETED);
    }

    @Test
    void abandonIfUnwatchedIsANoopWhenTheGameDoesNotExist() {
        gameService.abandonIfUnwatched("NOPE00");
    }
}
