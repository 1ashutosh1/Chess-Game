import { useState } from 'react'
import Button from '../components/Button'

function MultiplayerPage() {
  const [gameId, setGameId] = useState('')

  // Create/join wiring wait on the backend game endpoints — a later milestone.
  const handleCreateGame = () => {}
  const handleJoinGame = () => {}

  return (
    <main className="page">
      <h1>Play With Friend</h1>
      <Button onClick={handleCreateGame}>Create Game</Button>
      <div className="join-row">
        <input
          type="text"
          placeholder="Enter Game ID"
          value={gameId}
          onChange={(event) => setGameId(event.target.value)}
        />
        <Button onClick={handleJoinGame} disabled={!gameId.trim()}>
          Join
        </Button>
      </div>
      <Button to="/" variant="secondary">
        Back
      </Button>
    </main>
  )
}

export default MultiplayerPage
