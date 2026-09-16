// Every multiplayer WebSocket interaction goes through this file — hooks and
// components never touch the WebSocket API or the wire message shapes
// directly. The backend is the single source of truth for game state: this
// module only ships MOVE messages out and reports GAME_STATE/ERROR messages
// back in, it never interprets or applies them itself.
import { API_BASE_URL } from './gamesApi'

const WS_BASE_URL = API_BASE_URL.replace(/^http/, 'ws')

/**
 * Opens one WebSocket connection identifying a single game and player.
 * `handlers` may implement onOpen, onClose, onGameState(state),
 * onError(message) and onPlayerStatus(status) — all optional.
 *
 * Returns { sendMove(from, to), close() }.
 */
export function connectGameSocket(gameId, player, handlers = {}) {
  const url = `${WS_BASE_URL}/ws/game?gameId=${encodeURIComponent(gameId)}&player=${encodeURIComponent(player)}`
  const socket = new WebSocket(url)

  socket.onopen = () => handlers.onOpen?.()
  socket.onclose = () => handlers.onClose?.()

  socket.onmessage = (event) => {
    let data
    try {
      data = JSON.parse(event.data)
    } catch {
      return
    }

    if (data.type === 'GAME_STATE') {
      handlers.onGameState?.(data)
    } else if (data.type === 'ERROR') {
      handlers.onError?.(data.message)
    } else if (data.type === 'PLAYER_STATUS') {
      handlers.onPlayerStatus?.(data)
    }
  }

  return {
    sendMove(from, to) {
      socket.send(JSON.stringify({ type: 'MOVE', gameId, from, to }))
    },
    close() {
      socket.close()
    },
  }
}
