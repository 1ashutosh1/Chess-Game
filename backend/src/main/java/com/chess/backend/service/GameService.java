package com.chess.backend.service;

import com.chess.backend.model.Game;
import com.chess.backend.model.GameStatus;
import com.chess.backend.model.PlayerColor;
import com.chess.backend.repository.GameRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class GameService {

    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final String DEFAULT_WHITE_NAME = "White";
    private static final String DEFAULT_BLACK_NAME = "Black";

    private static final String GAME_ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GAME_ID_LENGTH = 6;

    private final GameRepository gameRepository;
    private final SecureRandom random = new SecureRandom();

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Creates a new game for the given player, seated as White, on the
     * standard starting position, and persists it via the repository.
     */
    public Game createGame(String whitePlayerName) {
        Game game = new Game(
                generateGameId(),
                nameOrDefault(whitePlayerName, DEFAULT_WHITE_NAME),
                STARTING_FEN,
                PlayerColor.WHITE,
                GameStatus.WAITING);

        return gameRepository.save(game);
    }

    /**
     * Seats the given player as Black on a waiting game and starts play.
     *
     * @throws ResponseStatusException 404 if no game exists with that id,
     *                                  409 if the game already has a Black player
     */
    public Game joinGame(String gameId, String blackPlayerName) {
        Game game = getGame(gameId);

        if (game.getBlackPlayer() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game " + gameId + " already has two players");
        }

        game.setBlackPlayer(nameOrDefault(blackPlayerName, DEFAULT_BLACK_NAME));
        game.setStatus(GameStatus.IN_PROGRESS);

        // Explicit save even though this is the same mutated instance the
        // in-memory repository already holds: once a real (JPA) repository
        // replaces it, mutating a fetched entity won't persist on its own,
        // so every write path must go through save().
        return gameRepository.save(game);
    }

    public Optional<Game> findGame(String gameId) {
        return gameRepository.findById(gameId);
    }

    /**
     * @throws ResponseStatusException 404 if no game exists with that id
     */
    public Game getGame(String gameId) {
        return findGame(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No game found with id " + gameId));
    }

    private static String nameOrDefault(String name, String defaultName) {
        return (name == null || name.isBlank()) ? defaultName : name;
    }

    private String generateGameId() {
        StringBuilder id = new StringBuilder(GAME_ID_LENGTH);
        for (int i = 0; i < GAME_ID_LENGTH; i++) {
            id.append(GAME_ID_ALPHABET.charAt(random.nextInt(GAME_ID_ALPHABET.length())));
        }
        return id.toString();
    }
}
