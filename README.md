# Chess Game

A full-stack chess MVP: play against a basic computer opponent, or play a
friend in real time over a shared game ID. The backend is the single source
of truth for legality — the frontend only ever renders what the server (or,
for single player, chess.js locally) confirms.

## Features

- **Single player** — play White against a computer opponent that picks a
  random legal move (chess.js drives local rules and move validation).
  The board position is saved to `localStorage`, so navigating away and
  coming back (or refreshing) resumes the same game instead of starting
  over. Only pressing **New Game** resets it.
- **Two-player multiplayer** — create a game and share its 6-character game
  ID with a friend, who joins via that ID. Moves are validated server-side
  (using the `chesslib` library) and broadcast to both players over a
  WebSocket connection — the frontend never decides whether a multiplayer
  move is legal.
- **Real-time play** — moves, check/checkmate/draw detection, and game
  status all propagate live over WebSocket; no polling.
- **Refresh / reconnect** — a multiplayer player who refreshes the page (or
  closes and reopens the tab) automatically resumes their game, as long as
  their browser still has its session in `localStorage`.
- **Opponent-disconnect awareness** — if your opponent's connection drops
  (e.g. they closed the tab or cleared their storage), you see an "Opponent
  disconnected" banner instead of a board that silently stops responding.
  Once *nobody* is left connected to an abandoned game, the backend marks it
  completed so the single game slot frees up for a new game.
- Legal move highlighting, check/checkmate/draw detection, and a turn
  indicator (a green arrow next to "You" when it's your move).

## Tech stack

**Frontend** — React 19 + Vite, `chess.js` (local rules for single player),
`react-chessboard` (board UI), the native `WebSocket` API (multiplayer),
`react-router-dom` (routing). Plain CSS, no UI framework.

**Backend** — Java 17 + Spring Boot (Web + WebSocket), `chesslib`
(server-side chess rules engine, pulled from JitPack), and an in-memory
repository (`AtomicReference`-backed) as the single active game's storage.

## Project structure

```
backend/
  src/main/java/com/chess/backend/
    controller/   REST endpoints (create/join/get a game)
    websocket/    WebSocket handshake + message handling
    service/      GameService — all chess/game business logic
    repository/   GameRepository interface + InMemoryGameRepository
    model/        Game, GameStatus, PlayerColor
    dto/          Wire-format records for REST responses and WS messages
    config/       CORS and WebSocket wiring
  src/test/java/... — unit tests for the above

frontend/
  src/
    pages/        HomePage, SinglePlayerPage, MultiplayerPage
    hooks/        useSinglePlayerGame, useMultiplayerGame
    components/   Button, Spinner
    services/     gamesApi (REST), gameSocket (WebSocket), gameStorage
    engine/       computerPlayer (random-legal-move opponent)
```

The backend follows a layered `GameService → GameRepository → InMemoryGameRepository`
design specifically so a JPA/PostgreSQL-backed repository can be dropped in
later without touching `GameService`'s business logic.

## Running locally

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. Run the tests with `./mvnw test`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` (Vite's default). Configure the backend URL
via a `.env` file (see `.env.example`):

```
VITE_API_BASE_URL=http://localhost:8080
```

## REST API

| Method | Endpoint                  | Description                          |
|--------|----------------------------|---------------------------------------|
| POST   | `/api/games`               | Create a game (creator seated White) |
| POST   | `/api/games/{gameId}/join` | Join a waiting game (seated Black)   |
| GET    | `/api/games/{gameId}`      | Fetch current game state             |

## WebSocket protocol

Connect to `ws://<host>/ws/game?gameId=<id>&player=<name>`.

**Client → server**
```json
{ "type": "MOVE", "gameId": "A7X29P", "from": "e2", "to": "e4" }
```

**Server → client**
```json
{ "type": "GAME_STATE", "gameId": "A7X29P", "fen": "...", "turn": "BLACK", "status": "IN_PROGRESS" }
{ "type": "ERROR", "message": "It is not Bob's turn" }
{ "type": "PLAYER_STATUS", "gameId": "A7X29P", "player": "WHITE", "connected": false }
```

## Known limitations (by design, for this MVP)

- **One active multiplayer game at a time**, server-wide (no per-user
  accounts or matchmaking). This keeps the backend simple, per the project's
  MVP scope.
- **No authentication** — a player's "identity" in a game is just the name
  string they created/joined with, matched at WebSocket handshake time.
  Anyone who knows a game's ID and a seated player's name could reconnect as
  that seat. Acceptable for a friends-only MVP, not for production.
- If a player's browser loses its session (e.g. `localStorage` is cleared)
  mid-game, they cannot rejoin that same game — there's no session token to
  recover. The remaining player is at least notified via the
  "Opponent disconnected" banner, and the game is freed up once nobody is
  left watching it.
- Single-player promotions always default to a queen (no underpromotion
  picker yet).
- The computer opponent picks a random legal move — no difficulty levels or
  actual engine evaluation.

## Future scope

- **Persistence** — swap `InMemoryGameRepository` for a JPA/PostgreSQL
  implementation of `GameRepository` to survive server restarts and support
  more than one concurrent game.
- **Accounts & matchmaking** — real authentication, a lobby/queue, and
  support for many simultaneous games instead of the single in-memory slot.
- **Rejoin flow** — let a player who lost their local session get back into
  an in-progress game (e.g. by re-entering the game ID and picking their
  seat), rather than only being able to abandon it.
- **Resign / draw offers** — explicit player-initiated ways to end a game,
  instead of relying only on checkmate/draw detection or disconnect-based
  abandonment.
- **Underpromotion** — let the player choose which piece to promote to,
  instead of always defaulting to a queen.
- **Stronger computer opponent** — replace the random-move single-player
  engine with a real evaluation-based engine and adjustable difficulty.
- **Move history / captured pieces UI**, spectator mode, and basic
  responsive/mobile polish.
