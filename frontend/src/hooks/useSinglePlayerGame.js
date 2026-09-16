import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Chess } from 'chess.js'
import { chooseComputerMove } from '../engine/computerPlayer'
import { clearSinglePlayerFen, loadSinglePlayerFen, saveSinglePlayerFen } from '../services/gameStorage'

const HUMAN_COLOR = 'w'
const COMPUTER_COLOR = 'b'

const SELECTED_SQUARE_STYLE = { backgroundColor: 'rgba(255, 255, 0, 0.4)' }
const LEGAL_MOVE_DOT_STYLE = {
  backgroundImage: 'radial-gradient(circle, rgba(0, 0, 0, 0.3) 20%, transparent 21%)',
}
const LEGAL_CAPTURE_STYLE = {
  boxShadow: 'inset 0 0 0 4px rgba(0, 0, 0, 0.3)',
}

function describeStatus(game, isComputerThinking) {
  const sideToMove = game.turn() === 'w' ? 'White' : 'Black'

  if (game.isCheckmate()) {
    const winner = game.turn() === 'w' ? 'Black' : 'White'
    return `Checkmate — ${winner} wins`
  }
  if (game.isStalemate()) return 'Draw — stalemate'
  if (game.isThreefoldRepetition()) return 'Draw — threefold repetition'
  if (game.isInsufficientMaterial()) return 'Draw — insufficient material'
  if (game.isDrawByFiftyMoves()) return 'Draw — fifty-move rule'
  if (game.isDraw()) return 'Draw'
  if (isComputerThinking) return 'Computer is thinking…'
  if (game.isCheck()) return `${sideToMove} to move — in check`
  return `${sideToMove} to move`
}

// Resumes whatever position was last saved, if any, so navigating away
// (e.g. the Back button) and returning doesn't lose the game. Falls back to
// a fresh board if nothing was saved or the saved FEN is corrupted.
function createInitialGame() {
  const savedFen = loadSinglePlayerFen()
  if (savedFen) {
    try {
      return new Chess(savedFen)
    } catch {
      clearSinglePlayerFen()
    }
  }
  return new Chess()
}

// Human plays White, computer plays Black. Every computer reply goes
// through chooseComputerMove() — swapping that one function for a stronger
// engine later requires no changes here.
export function useSinglePlayerGame() {
  const gameRef = useRef(createInitialGame())
  const [fen, setFen] = useState(gameRef.current.fen())
  const [selectedSquare, setSelectedSquare] = useState(null)
  const [legalMoves, setLegalMoves] = useState([])
  const [isComputerThinking, setIsComputerThinking] = useState(false)

  const status = useMemo(
    () => describeStatus(gameRef.current, isComputerThinking),
    [fen, isComputerThinking],
  )
  const isGameOver = gameRef.current.isGameOver()
  const isHumanTurn = gameRef.current.turn() === HUMAN_COLOR && !isComputerThinking && !isGameOver

  useEffect(() => {
    saveSinglePlayerFen(fen)
  }, [fen])

  const clearSelection = useCallback(() => {
    setSelectedSquare(null)
    setLegalMoves([])
  }, [])

  // Let the computer reply whenever it becomes its turn.
  useEffect(() => {
    const game = gameRef.current
    if (game.turn() !== COMPUTER_COLOR || game.isGameOver()) return

    let cancelled = false
    setIsComputerThinking(true)

    chooseComputerMove(game).then((move) => {
      if (cancelled) return
      // Apply the move and clear the "thinking" flag in the same batch —
      // setting isComputerThinking(false) from a separate .finally() would
      // race the cleanup this fen update itself triggers.
      if (move) {
        game.move(move.san)
        setFen(game.fen())
      }
      setIsComputerThinking(false)
    })

    return () => {
      cancelled = true
    }
  }, [fen])

  const selectSquare = useCallback(
    (square) => {
      if (!isHumanTurn) return
      const moves = gameRef.current.moves({ square, verbose: true })
      if (moves.length === 0) {
        clearSelection()
        return
      }
      setSelectedSquare(square)
      setLegalMoves(moves)
    },
    [clearSelection, isHumanTurn],
  )

  const attemptMove = useCallback(
    (from, to) => {
      if (!isHumanTurn) return false
      const game = gameRef.current
      try {
        // Promotions always default to a queen — pawn-promotion choice is a
        // later refinement, not needed for the MVP.
        const move = game.move({ from, to, promotion: 'q' })
        if (!move) return false
      } catch {
        return false
      }
      setFen(game.fen())
      clearSelection()
      return true
    },
    [clearSelection, isHumanTurn],
  )

  const onPieceDrop = useCallback(
    ({ sourceSquare, targetSquare }) => {
      if (!targetSquare) {
        clearSelection()
        return false
      }
      return attemptMove(sourceSquare, targetSquare)
    },
    [attemptMove, clearSelection],
  )

  const onPieceDrag = useCallback(
    ({ square }) => {
      if (square) selectSquare(square)
    },
    [selectSquare],
  )

  const onSquareClick = useCallback(
    ({ square, piece }) => {
      if (!isHumanTurn) return
      const game = gameRef.current

      if (selectedSquare && legalMoves.some((move) => move.to === square)) {
        attemptMove(selectedSquare, square)
        return
      }

      if (square === selectedSquare) {
        clearSelection()
        return
      }

      if (piece && piece.pieceType[0] === game.turn()) {
        selectSquare(square)
      } else {
        clearSelection()
      }
    },
    [isHumanTurn, selectedSquare, legalMoves, attemptMove, selectSquare, clearSelection],
  )

  const newGame = useCallback(() => {
    gameRef.current.reset()
    setFen(gameRef.current.fen())
    setIsComputerThinking(false)
    clearSelection()
  }, [clearSelection])

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

  const chessboardOptions = {
    id: 'single-player-game',
    position: fen,
    onPieceDrop,
    onPieceDrag,
    onSquareClick,
    squareStyles,
    allowDragging: isHumanTurn,
  }

  return { status, chessboardOptions, newGame }
}
