import { useState } from 'react'
import { Chessboard } from 'react-chessboard'
import Button from '../components/Button'
import Spinner from '../components/Spinner'
import { useMultiplayerGame } from '../hooks/useMultiplayerGame'

function MultiplayerPage() {
  const [gameIdInput, setGameIdInput] = useState('')
  const {
    game,
    error,
    isLoading,
    isRestoring,
    connectionLabel,
    createGame,
    joinGame,
    chessboardOptions,
    colorLabel,
    statusLabel,
    isYourTurn,
  } = useMultiplayerGame()

  const handleJoinSubmit = (event) => {
    event.preventDefault()
    joinGame(gameIdInput.trim().toUpperCase())
  }

  return (
    <main className="page">
      {isRestoring ? (
        <Spinner />
      ) : game ? (
        <>
          <dl className="game-info">
            <div className="game-info-row">
              <dt>Game ID</dt>
              <dd>{game.gameId}</dd>
            </div>
            <div className="game-info-row">
              <dt>You</dt>
              <dd>
                {colorLabel}
                {isYourTurn && (
                  <span className="turn-arrow" title="Your turn" aria-label="Your turn">
                    &#9664;
                  </span>
                )}
              </dd>
            </div>
            <div className="game-info-row">
              <dt>Status</dt>
              <dd>{statusLabel}</dd>
            </div>
          </dl>
          {connectionLabel && <p className="connection-status">{connectionLabel}</p>}
          {error && <p className="error">{error}</p>}
          <div className="board-wrapper">
            <Chessboard options={chessboardOptions} />
          </div>
          <div className="back-row">
            <Button to="/" variant="secondary">
              Back
            </Button>
          </div>
        </>
      ) : (
        <>
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
          <div className="back-row">
            <Button to="/" variant="secondary">
              Back
            </Button>
          </div>
        </>
      )}
    </main>
  )
}

export default MultiplayerPage
