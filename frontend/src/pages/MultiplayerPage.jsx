import { useState } from 'react'
import Button from '../components/Button'
import { useMultiplayerGame } from '../hooks/useMultiplayerGame'

function MultiplayerPage() {
  const [gameIdInput, setGameIdInput] = useState('')
  const { game, error, isLoading, createGame, joinGame, colorLabel, statusLabel } =
    useMultiplayerGame()

  const handleJoinSubmit = (event) => {
    event.preventDefault()
    joinGame(gameIdInput.trim().toUpperCase())
  }

  if (game) {
    return (
      <main className="page">
        <h1>Play With Friend</h1>
        <p>
          Game ID: <strong>{game.gameId}</strong>
        </p>
        <p>
          You are: <strong>{colorLabel}</strong>
        </p>
        <p className="status">{statusLabel}</p>
        <Button to="/" variant="secondary">
          Back
        </Button>
      </main>
    )
  }

  return (
    <main className="page">
      <h1>Play With Friend</h1>
      {error && <p className="error">{error}</p>}
      <Button onClick={createGame} disabled={isLoading}>
        Create Game
      </Button>
      <form className="join-row" onSubmit={handleJoinSubmit}>
        <input
          type="text"
          placeholder="Enter Game ID"
          value={gameIdInput}
          onChange={(event) => setGameIdInput(event.target.value)}
          disabled={isLoading}
        />
        <Button type="submit" disabled={isLoading || !gameIdInput.trim()}>
          Join
        </Button>
      </form>
      <Button to="/" variant="secondary">
        Back
      </Button>
    </main>
  )
}

export default MultiplayerPage
