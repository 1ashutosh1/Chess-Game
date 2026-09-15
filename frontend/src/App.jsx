import { useCallback, useMemo, useRef, useState } from 'react'
import { Chess } from 'chess.js'
import { Chessboard } from 'react-chessboard'
import './App.css'

const SELECTED_SQUARE_STYLE = { backgroundColor: 'rgba(255, 255, 0, 0.4)' }
const LEGAL_MOVE_DOT_STYLE = {
  backgroundImage: 'radial-gradient(circle, rgba(0, 0, 0, 0.3) 20%, transparent 21%)',
}
const LEGAL_CAPTURE_STYLE = {
  boxShadow: 'inset 0 0 0 4px rgba(0, 0, 0, 0.3)',
}

function describeStatus(game) {
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
  if (game.isCheck()) return `${sideToMove} to move — in check`
  return `${sideToMove} to move`
}

function App() {
  const gameRef = useRef(new Chess())
  const [fen, setFen] = useState(gameRef.current.fen())
  const [selectedSquare, setSelectedSquare] = useState(null)
  const [legalMoves, setLegalMoves] = useState([])

  const status = useMemo(() => describeStatus(gameRef.current), [fen])
  const isGameOver = gameRef.current.isGameOver()

  const clearSelection = useCallback(() => {
    setSelectedSquare(null)
    setLegalMoves([])
  }, [])

  const selectSquare = useCallback(
    (square) => {
      const moves = gameRef.current.moves({ square, verbose: true })
      if (moves.length === 0) {
        clearSelection()
        return
      }
      setSelectedSquare(square)
      setLegalMoves(moves)
    },
    [clearSelection],
  )

  const attemptMove = useCallback(
    (from, to) => {
      const game = gameRef.current
      try {
        // Promotions always default to a queen — pawn-promotion choice is a
        // later refinement, not needed for the local-play MVP.
        const move = game.move({ from, to, promotion: 'q' })
        if (!move) return false
      } catch {
        return false
      }
      setFen(game.fen())
      clearSelection()
      return true
    },
    [clearSelection],
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
    [selectedSquare, legalMoves, attemptMove, selectSquare, clearSelection],
  )

  const handleNewGame = () => {
    gameRef.current.reset()
    setFen(gameRef.current.fen())
    clearSelection()
  }

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
    id: 'local-game',
    position: fen,
    onPieceDrop,
    onPieceDrag,
    onSquareClick,
    squareStyles,
    allowDragging: !isGameOver,
  }

  return (
    <main>
      <h1 className="status">{status}</h1>
      <div className="board-wrapper">
        <Chessboard options={chessboardOptions} />
      </div>
      <button type="button" onClick={handleNewGame}>
        New Game
      </button>
    </main>
  )
}

export default App
