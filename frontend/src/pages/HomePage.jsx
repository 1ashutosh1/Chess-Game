import Button from '../components/Button'

function HomePage() {
  return (
    <main className="page page-home">
      <h1>Chess</h1>
      <div className="button-stack">
        <Button to="/single-player">Play Against Computer</Button>
        <Button to="/multiplayer">Play With Friend</Button>
      </div>
    </main>
  )
}

export default HomePage
