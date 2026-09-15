import { Route, Routes } from 'react-router-dom'
import HomePage from './pages/HomePage'
import SinglePlayerPage from './pages/SinglePlayerPage'
import MultiplayerPage from './pages/MultiplayerPage'
import './App.css'

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/single-player" element={<SinglePlayerPage />} />
      <Route path="/multiplayer" element={<MultiplayerPage />} />
    </Routes>
  )
}

export default App
