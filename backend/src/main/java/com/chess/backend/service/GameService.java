package com.chess.backend.service;

import com.chess.backend.model.Game;
import com.chess.backend.model.GameStatus;
import com.chess.backend.model.PlayerColor;
import com.chess.backend.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class GameService {

    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

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
    public Game createGame(String whitePlayer) {
        Game game = new Game(
                generateGameId(),
                whitePlayer,
                STARTING_FEN,
                PlayerColor.WHITE,
                GameStatus.WAITING_FOR_OPPONENT);

        return gameRepository.save(game);
    }

    public Optional<Game> findGame(String gameId) {
        return gameRepository.findById(gameId);
    }

    private String generateGameId() {
        StringBuilder id = new StringBuilder(GAME_ID_LENGTH);
        for (int i = 0; i < GAME_ID_LENGTH; i++) {
            id.append(GAME_ID_ALPHABET.charAt(random.nextInt(GAME_ID_ALPHABET.length())));
        }
        return id.toString();
    }
}
