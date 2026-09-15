import { Chessboard } from 'react-chessboard'
import Button from '../components/Button'
import { useSinglePlayerGame } from '../hooks/useSinglePlayerGame'

function SinglePlayerPage() {
  const { status, chessboardOptions, newGame } = useSinglePlayerGame()

  return (
    <main className="page">
      <h1 className="status">{status}</h1>
      <div className="board-wrapper">
        <Chessboard options={chessboardOptions} />
      </div>
      <div className="button-row">
        <Button onClick={newGame}>New Game</Button>
        <Button to="/" variant="secondary">
          Back
        </Button>
      </div>
    </main>
  )
}

export default SinglePlayerPage
