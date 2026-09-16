package com.chess.backend.service;

import com.chess.backend.model.Game;
import com.chess.backend.model.GameStatus;
import com.chess.backend.model.PlayerColor;
import com.chess.backend.repository.GameRepository;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
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
     *
     * @throws ResponseStatusException 409 if a game is already waiting or in
     *                                  progress — this MVP has room for only
     *                                  one active game at a time, so a new
     *                                  one can't be created until it finishes
     */
    public Game createGame(String whitePlayerName) {
        gameRepository.findCurrent()
                .filter(game -> game.getStatus() != GameStatus.COMPLETED)
                .ifPresent(game -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "A game is already in progress. Try again once it finishes.");
                });

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

    /**
     * Validates and applies a move using {@code chesslib} as the authoritative
     * rules engine, then persists the resulting FEN, turn and status. The
     * frontend only ever sees the outcome of this method — it never decides
     * legality itself.
     *
     * @throws ResponseStatusException 404 if no game exists with that id,
     *                                  403 if the player is not seated in the game,
     *                                  409 if the game is not in progress or it is not the player's turn,
     *                                  400 if the move is not legal in the current position
     */
    public Game makeMove(String gameId, String player, String from, String to) {
        Game game = getGame(gameId);
        PlayerColor playerColor = colorOf(game, player);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game " + gameId + " is not in progress");
        }

        if (playerColor != game.getTurn()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "It is not " + player + "'s turn");
        }

        Board board = new Board();
        board.loadFromFen(game.getFen());

        Move move = findLegalMove(board, from, to);
        if (move == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal move " + from + "-" + to);
        }
        board.doMove(move);

        game.setFen(board.getFen());
        game.setTurn(board.getSideToMove() == Side.WHITE ? PlayerColor.WHITE : PlayerColor.BLACK);
        game.setStatus(board.isMated() || board.isDraw() ? GameStatus.COMPLETED : GameStatus.IN_PROGRESS);

        return gameRepository.save(game);
    }

    private static PlayerColor colorOf(Game game, String player) {
        if (player != null && player.equals(game.getWhitePlayer())) {
            return PlayerColor.WHITE;
        }
        if (player != null && player.equals(game.getBlackPlayer())) {
            return PlayerColor.BLACK;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, player + " is not a player in game " + game.getGameId());
    }

    /**
     * Looks up the matching move among chesslib's own legal moves for the
     * current position, rather than hand-validating from/to squares —
     * {@code Board.doMove} only re-checks that a move doesn't leave the king
     * in check, it does not verify a piece can reach that square at all, so
     * legality has to come from the generated move list. When the target
     * square is a promotion, defaults to promoting to a queen since the
     * frontend does not yet offer underpromotion.
     */
    private static Move findLegalMove(Board board, String from, String to) {
        Square fromSquare = parseSquare(from);
        Square toSquare = parseSquare(to);

        Move queenPromotion = null;
        for (Move candidate : board.legalMoves()) {
            if (candidate.getFrom() != fromSquare || candidate.getTo() != toSquare) {
                continue;
            }
            if (candidate.getPromotion() == Piece.NONE) {
                return candidate;
            }
            if (candidate.getPromotion().getPieceType() == PieceType.QUEEN) {
                queenPromotion = candidate;
            }
        }
        return queenPromotion;
    }

    private static Square parseSquare(String square) {
        try {
            return Square.valueOf(square.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid square: " + square);
        }
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
