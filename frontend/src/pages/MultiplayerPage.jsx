import { useState } from 'react'
import { Chessboard } from 'react-chessboard'
import Button from '../components/Button'
import { useMultiplayerGame } from '../hooks/useMultiplayerGame'

function MultiplayerPage() {
  const [gameIdInput, setGameIdInput] = useState('')
  const {
    game,
    error,
    isLoading,
    connectionLabel,
    createGame,
    joinGame,
    chessboardOptions,
    colorLabel,
    statusLabel,
  } = useMultiplayerGame()

  const handleJoinSubmit = (event) => {
    event.preventDefault()
    joinGame(gameIdInput.trim().toUpperCase())
  }

  if (game) {
    return (
      <main className="page">
        <h1 className="status">{statusLabel}</h1>
        <p>
          Game ID: <strong>{game.gameId}</strong> — You are: <strong>{colorLabel}</strong>
        </p>
        {connectionLabel && <p className="connection-status">{connectionLabel}</p>}
        {error && <p className="error">{error}</p>}
        <div className="board-wrapper">
          <Chessboard options={chessboardOptions} />
        </div>
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
