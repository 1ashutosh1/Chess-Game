// The computer opponent's entire surface area: given the current game,
// return the move it wants to make (or null if it has none).
//
// Declared `async` even though this implementation is instant — a future
// engine (minimax search, a Stockfish web worker) is inherently async, so
// callers already await this and won't need to change when it's swapped in.
//
// `game` is a chess.js Chess instance; it is only read (via `moves()`),
// never mutated — applying the returned move is the caller's job.
export async function chooseComputerMove(game) {
  const moves = game.moves({ verbose: true })
  if (moves.length === 0) return null

  return moves[Math.floor(Math.random() * moves.length)]
}
