import { useCallback, useState } from 'react'
import { createGame, joinGame } from '../services/gamesApi'

const COLOR_LABELS = { WHITE: 'White', BLACK: 'Black' }
const STATUS_LABELS = {
  WAITING: 'Waiting for opponent',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
}

// Drives the "Play With Friend" flow: create or join a game over REST, and
// hold onto just enough state to display it. No WebSocket yet, so the
// status shown here is a one-time snapshot from the create/join response,
// not a live feed.
export function useMultiplayerGame() {
  const [game, setGame] = useState(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(false)

  const handleCreateGame = useCallback(async () => {
    setError(null)
    setIsLoading(true)
    try {
      const response = await createGame()
      setGame({ gameId: response.gameId, color: 'WHITE', status: response.status })
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
      setGame({ gameId: response.gameId, color: 'BLACK', status: response.status })
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    game,
    error,
    isLoading,
    createGame: handleCreateGame,
    joinGame: handleJoinGame,
    colorLabel: game ? (COLOR_LABELS[game.color] ?? game.color) : null,
    statusLabel: game ? (STATUS_LABELS[game.status] ?? game.status) : null,
  }
}
