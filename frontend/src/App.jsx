import { useEffect, useState } from 'react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

function App() {
  const [status, setStatus] = useState('checking...')

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/health`)
      .then((res) => {
        if (!res.ok) {
          throw new Error(`Backend responded with ${res.status}`)
        }
        return res.json()
      })
      .then((data) => setStatus(data.status ?? 'unknown'))
      .catch((err) => setStatus(`unreachable (${err.message})`))
  }, [])

  return (
    <main>
      <h1>Chess MVP</h1>
      <p>Frontend is running.</p>
      <p>
        Backend health: <strong>{status}</strong>
      </p>
    </main>
  )
}

export default App
