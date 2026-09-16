// Persists just enough to resume a multiplayer game after a page refresh —
// the gameId and which color this browser is playing. Everything else (fen,
// turn, status) is always re-fetched from the backend on restore, never
// trusted from storage.
const STORAGE_KEY = 'chess.multiplayer.session'

export function saveSession(gameId, color) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ gameId, color }))
  } catch {
    // localStorage can be unavailable (private browsing, quota) — refresh
    // restore is a convenience, not a requirement, so fail silently.
  }
}

export function loadSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return parsed?.gameId && parsed?.color ? parsed : null
  } catch {
    return null
  }
}

export function clearSession() {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // ignore
  }
}

// Persists the single-player board position so navigating away (e.g. the
// Back button) and returning resumes the same game instead of starting
// over. The FEN is client-authoritative here (there's no backend for single
// player), so it's safe to trust directly from storage on restore.
const SINGLE_PLAYER_STORAGE_KEY = 'chess.singleplayer.fen'

export function saveSinglePlayerFen(fen) {
  try {
    localStorage.setItem(SINGLE_PLAYER_STORAGE_KEY, fen)
  } catch {
    // localStorage can be unavailable (private browsing, quota) — resuming
    // a game is a convenience, not a requirement, so fail silently.
  }
}

export function loadSinglePlayerFen() {
  try {
    return localStorage.getItem(SINGLE_PLAYER_STORAGE_KEY)
  } catch {
    return null
  }
}

export function clearSinglePlayerFen() {
  try {
    localStorage.removeItem(SINGLE_PLAYER_STORAGE_KEY)
  } catch {
    // ignore
  }
}
