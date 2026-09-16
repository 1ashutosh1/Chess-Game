import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Chess } from 'chess.js'
import { createGame, getGame, joinGame } from '../services/gamesApi'
import { connectGameSocket } from '../services/gameSocket'
import { clearSession, loadSession, saveSession } from '../services/gameStorage'

const COLOR_LABELS = { WHITE: 'White', BLACK: 'Black' }
// COMPLETED isn't here — its label always comes from describeGameEnd below,
// which knows the actual reason the game ended.
const STATUS_LABELS = {
  WAITING: 'Waiting for opponent',
  IN_PROGRESS: 'In progress',
}
const CONNECTION_LABELS = {
  CLOSED: 'Disconnected from game server',
}

const SELECTED_SQUARE_STYLE = { backgroundColor: 'rgba(255, 255, 0, 0.4)' }
const LEGAL_MOVE_DOT_STYLE = {
  backgroundImage: 'radial-gradient(circle, rgba(0, 0, 0, 0.3) 20%, transparent 21%)',
}
const LEGAL_CAPTURE_STYLE = {
  boxShadow: 'inset 0 0 0 4px rgba(0, 0, 0, 0.3)',
}

// The backend's COMPLETED status doesn't say why the game ended — it covers
// checkmate, every draw type, and a player disconnecting before any of those
// happened. Derive the actual reason client-side from the final position,
// the same way useSinglePlayerGame derives its status text.
function describeGameEnd(position) {
  if (position.isCheckmate()) {
    const winner = position.turn() === 'w' ? 'Black' : 'White'
    return `Checkmate — ${winner} wins`
  }
  if (position.isStalemate()) return 'Draw — stalemate'
  if (position.isThreefoldRepetition()) return 'Draw — threefold repetition'
  if (position.isInsufficientMaterial()) return 'Draw — insufficient material'
  if (position.isDrawByFiftyMoves()) return 'Draw — fifty-move rule'
  if (position.isDraw()) return 'Draw'
  // None of the above — the position is still playable, so COMPLETED here
  // can only mean a player disconnected before the game actually ended.
  // Worded differently from opponentStatusLabel's live "Opponent
  // disconnected" banner, since this one is the permanent final status.
  return 'Ended — opponent disconnected'
}

// Drives the "Play With Friend" flow: create or join a game over REST, then
// hand off to a WebSocket connection for live play. The backend is the only
// authority on the position — this hook never applies a move itself, it only
// renders whatever GAME_STATE the server last broadcast.
export function useMultiplayerGame() {
  const [game, setGame] = useState(null) // { gameId, color, player, fen, turn, status }
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  // null while a connection attempt is in flight, then 'OPEN' or 'CLOSED' —
  // driven entirely by the socket's own onOpen/onClose events, never set
  // synchronously by the effect itself (see handleCreateGame/handleJoinGame,
  // which reset it to null in the same event that starts a new game).
  const [connectionStatus, setConnectionStatus] = useState(null)
  // Whether the opponent's seat currently has any open WebSocket session,
  // per the backend's PLAYER_STATUS broadcasts — not this browser's own
  // connection (see connectionStatus above).
  const [opponentConnected, setOpponentConnected] = useState(true)
  const [selectedSquare, setSelectedSquare] = useState(null)
  // Read once, at mount, and never again — this is what makes the restore
  // effect below run exactly once regardless of later `game` changes.
  const [initialSession] = useState(() => loadSession())
  const [isRestoring, setIsRestoring] = useState(() => Boolean(initialSession))
  const socketRef = useRef(null)

  // On page load, resume whatever game this browser was last playing. Only
  // gameId/color come from storage — fen/turn/status are always re-fetched
  // from the backend, never trusted from a stale local copy. Setting `game`
  // here reuses the exact same WebSocket-connect effect below as create/join
  // do, so this is also how reconnection happens after a refresh.
  useEffect(() => {
    if (!initialSession) return undefined

    let cancelled = false
    getGame(initialSession.gameId)
      .then((response) => {
        if (cancelled) return
        if (response.status === 'COMPLETED') {
          // The game ended (checkmate, draw, or abandonment) while this
          // browser was away — don't resume into a dead board, land on the
          // create/join screen instead.
          clearSession()
          return
        }
        setOpponentConnected(true)
        setGame({
          gameId: response.gameId,
          color: initialSession.color,
          player: initialSession.color === 'WHITE' ? response.whitePlayer : response.blackPlayer,
          fen: response.fen,
          turn: response.turn,
          status: response.status,
        })
      })
      .catch((err) => {
        if (cancelled) return
        // Most likely the game is gone (server restarted, or it simply no
        // longer exists) — drop the stale session so we stop trying.
        clearSession()
        setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setIsRestoring(false)
      })

    return () => {
      cancelled = true
    }
  }, [initialSession])

  // One WebSocket connection per (gameId, player) pair. GAME_STATE updates
  // mutate `game` in place without changing gameId/player, so they don't
  // retrigger this effect — only starting a genuinely new game does.
  useEffect(() => {
    if (!game) return undefined

    const socket = connectGameSocket(game.gameId, game.player, {
      onOpen: () => setConnectionStatus('OPEN'),
      onClose: () => setConnectionStatus('CLOSED'),
      onGameState: (state) => {
        setError(null)
        setSelectedSquare(null)
        setGame((current) =>
          current ? { ...current, fen: state.fen, turn: state.turn, status: state.status } : current,
        )
      },
      onError: (message) => {
        setError(message)
        setSelectedSquare(null)
      },
      onPlayerStatus: (status) => {
        if (status.player !== game.color) {
          setOpponentConnected(status.connected)
        }
      },
    })
    socketRef.current = socket

    return () => {
      socket.close()
      socketRef.current = null
    }
  }, [game?.gameId, game?.player])

  const handleCreateGame = useCallback(async () => {
    setError(null)
    setIsLoading(true)
    try {
      const response = await createGame()
      saveSession(response.gameId, 'WHITE')
      setConnectionStatus(null)
      setOpponentConnected(true)
      setSelectedSquare(null)
      setGame({
        gameId: response.gameId,
        color: 'WHITE',
        player: response.whitePlayer,
        fen: response.fen,
        turn: response.turn,
        status: response.status,
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const handleJoinGame = useCallback(async (gameId) => {
    setError(null)
    setIsLoading(true)
    try {
      const response = await joinGame(gameId)
      saveSession(response.gameId, 'BLACK')
      setConnectionStatus(null)
      setOpponentConnected(true)
      setSelectedSquare(null)
      setGame({
        gameId: response.gameId,
        color: 'BLACK',
        player: response.blackPlayer,
        fen: response.fen,
        turn: response.turn,
        status: response.status,
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const canMove = Boolean(game) && connectionStatus === 'OPEN' && game.status === 'IN_PROGRESS' && game.turn === game.color

  // Read-only: reflects the authoritative FEN, never mutated by a local
  // move. Used only to compute legal-move highlights for the player whose
  // turn it already is according to the backend.
  const localPosition = useMemo(() => (game ? new Chess(game.fen) : null), [game?.fen])

  const legalMoves = useMemo(() => {
    if (!localPosition || !selectedSquare) return []
    return localPosition.moves({ square: selectedSquare, verbose: true })
  }, [localPosition, selectedSquare])

  const clearSelection = useCallback(() => setSelectedSquare(null), [])

  const sendMove = useCallback(
    (from, to) => {
      if (!canMove) return
      socketRef.current?.sendMove(from, to)
      clearSelection()
    },
    [canMove, clearSelection],
  )

  const selectSquare = useCallback(
    (square) => {
      if (!canMove || !localPosition) return
      const moves = localPosition.moves({ square, verbose: true })
      if (moves.length === 0) {
        clearSelection()
        return
      }
      setSelectedSquare(square)
    },
    [canMove, localPosition, clearSelection],
  )

  const onPieceDrop = useCallback(
    ({ sourceSquare, targetSquare }) => {
      if (targetSquare && canMove) {
        sendMove(sourceSquare, targetSquare)
      } else {
        clearSelection()
      }
      // Never optimistically apply the move on the board — it only ever
      // moves once the backend's GAME_STATE broadcast confirms it, so the
      // drop is always reported as not-yet-applied.
      return false
    },
    [canMove, sendMove, clearSelection],
  )

  const onPieceDrag = useCallback(({ square }) => {
    if (square) selectSquare(square)
  }, [selectSquare])

  const onSquareClick = useCallback(
    ({ square, piece }) => {
      if (!canMove) return

      if (selectedSquare && legalMoves.some((move) => move.to === square)) {
        sendMove(selectedSquare, square)
        return
      }

      if (square === selectedSquare) {
        clearSelection()
        return
      }

      if (piece && piece.pieceType[0] === game.color[0].toLowerCase()) {
        selectSquare(square)
      } else {
        clearSelection()
      }
    },
    [canMove, selectedSquare, legalMoves, sendMove, clearSelection, selectSquare, game],
  )

  const squareStyles = useMemo(() => {
    const styles = {}
    if (selectedSquare) {
      styles[selectedSquare] = SELECTED_SQUARE_STYLE
    }
    for (const move of legalMoves) {
      styles[move.to] = move.captured ? LEGAL_CAPTURE_STYLE : LEGAL_MOVE_DOT_STYLE
    }
    return styles
  }, [selectedSquare, legalMoves])

  const chessboardOptions = game && {
    id: 'multiplayer-game',
    position: game.fen,
    onPieceDrop,
    onPieceDrag,
    onSquareClick,
    squareStyles,
    allowDragging: canMove,
  }

  return {
    game,
    error,
    isLoading: isLoading || isRestoring,
    isRestoring,
    connectionStatus,
    connectionLabel:
      !game || connectionStatus === 'OPEN'
        ? null
        : (CONNECTION_LABELS[connectionStatus] ?? 'Connecting to game server…'),
    createGame: handleCreateGame,
    joinGame: handleJoinGame,
    chessboardOptions,
    colorLabel: game ? (COLOR_LABELS[game.color] ?? game.color) : null,
    statusLabel: game
      ? game.status === 'COMPLETED'
        ? describeGameEnd(localPosition)
        : (STATUS_LABELS[game.status] ?? game.status)
      : null,
    isYourTurn: Boolean(game) && game.status === 'IN_PROGRESS' && game.turn === game.color,
    opponentStatusLabel: game && !opponentConnected ? 'Opponent disconnected' : null,
  }
}
