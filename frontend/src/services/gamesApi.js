// Every call to the backend's /api/games endpoints goes through this file —
// components never call fetch() or know the base URL directly.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function request(path, options) {
  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, options)
  } catch {
    throw new Error('Unable to reach the server. Please check your connection and try again.')
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message ?? `Request failed (${response.status})`)
  }

  return response.json()
}

export function createGame(playerName) {
  return request('/api/games', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ playerName }),
  })
}

export function joinGame(gameId, playerName) {
  return request(`/api/games/${encodeURIComponent(gameId)}/join`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ playerName }),
  })
}

export function getGame(gameId) {
  return request(`/api/games/${encodeURIComponent(gameId)}`)
}
