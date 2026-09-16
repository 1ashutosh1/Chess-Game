import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Chess } from 'chess.js'
import { createGame, joinGame } from '../services/gamesApi'
import { connectGameSocket } from '../services/gameSocket'

const COLOR_LABELS = { WHITE: 'White', BLACK: 'Black' }
const STATUS_LABELS = {
  WAITING: 'Waiting for opponent',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
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
  const [selectedSquare, setSelectedSquare] = useState(null)
  const socketRef = useRef(null)

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
      setConnectionStatus(null)
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
      setConnectionStatus(null)
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
    isLoading,
    connectionStatus,
    connectionLabel:
      !game || connectionStatus === 'OPEN'
        ? null
        : (CONNECTION_LABELS[connectionStatus] ?? 'Connecting to game server…'),
    createGame: handleCreateGame,
    joinGame: handleJoinGame,
    chessboardOptions,
    colorLabel: game ? (COLOR_LABELS[game.color] ?? game.color) : null,
    statusLabel: game ? (STATUS_LABELS[game.status] ?? game.status) : null,
  }
}
