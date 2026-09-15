package com.chess.backend.model;

/**
 * A single two-player chess game. Deliberately free of any persistence or
 * transport concerns (no JPA annotations, no DTO mapping) so it can move
 * between storage backends and over the wire unchanged.
 */
public class Game {

    private final String gameId;
    private final String whitePlayer;
    private String blackPlayer;
    private String fen;
    private PlayerColor turn;
    private GameStatus status;

    public Game(String gameId, String whitePlayer, String fen, PlayerColor turn, GameStatus status) {
        this.gameId = gameId;
        this.whitePlayer = whitePlayer;
        this.fen = fen;
        this.turn = turn;
        this.status = status;
    }

    public String getGameId() {
        return gameId;
    }

    public String getWhitePlayer() {
        return whitePlayer;
    }

    public String getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(String blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    public PlayerColor getTurn() {
        return turn;
    }

    public void setTurn(PlayerColor turn) {
        this.turn = turn;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }
}
