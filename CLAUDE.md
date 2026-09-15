# Chess MVP - Development Guidelines

## Goal

Build a functional chess web application MVP within a very limited development
time window.

The application must support:

1. Single-player chess against a basic computer opponent.
2. Two-player multiplayer chess through a shared game ID.
3. Real-time multiplayer moves using WebSocket.
4. Legal chess move validation.
5. Check, checkmate and draw detection.
6. Browser refresh/reconnection for multiplayer games.
7. Deployment-ready frontend and backend.

The primary goal is functional correctness and a clean, explainable architecture.
Do not spend excessive time on visual polish.

---

## Technology

### Frontend

- React
- Vite
- JavaScript
- chess.js
- react-chessboard
- Native WebSocket API

### Backend

- Java
- Spring Boot
- Spring Web
- Spring WebSocket
- A mature Java chess library for server-side move validation
- ConcurrentHashMap for initial persistence

### Future persistence

The backend must be designed so that PostgreSQL/JPA can be introduced later
without changing the GameService business logic.

Use:

GameService
    ↓
GameRepository interface
    ↓
InMemoryGameRepository

Future:

GameService
    ↓
GameRepository interface
    ↓
JpaGameRepository
    ↓
PostgreSQL

---

## Architecture Rules

Keep the backend simple.

Preferred structure:

backend/
  controller/
  websocket/
  service/
  repository/
  model/
  dto/
  config/

Do not introduce unnecessary:
- microservices
- factories
- strategy patterns
- event buses
- Kafka
- Redis
- authentication
- database
- complex dependency injection abstractions

unless explicitly requested.

---

## Multiplayer Rules

The backend is authoritative.

The frontend must NOT be trusted to determine whether a multiplayer move
is legal.

Flow:

Client
  ↓
WebSocket
  ↓
Backend
  ↓
Validate player
  ↓
Validate turn
  ↓
Validate chess move
  ↓
Update game
  ↓
Broadcast updated state

Invalid moves must be rejected by the backend.

---

## Game State

A game should contain approximately:

- gameId
- whitePlayer
- blackPlayer
- current FEN
- turn
- status
- lastActivity

Avoid coupling the Game model directly to database-specific concepts.

---

## In-Memory Storage

Use:

ConcurrentHashMap<String, Game>

through InMemoryGameRepository.

Games should have an expiration/cleanup mechanism so abandoned games do not
remain in memory indefinitely.

---

## REST API

Keep REST APIs minimal.

Expected endpoints:

POST /api/games
POST /api/games/{gameId}/join
GET  /api/games/{gameId}

Additional endpoints require justification.

---

## WebSocket

Use a simple native WebSocket protocol rather than introducing unnecessary
messaging complexity.

Messages should be explicit and easy to debug.

Example client → server:

{
  "type": "MOVE",
  "gameId": "A7X29P",
  "from": "e2",
  "to": "e4"
}

Example server → client:

{
  "type": "GAME_STATE",
  "gameId": "A7X29P",
  "fen": "...",
  "turn": "BLACK",
  "status": "IN_PROGRESS"
}

Handle:

- connection
- join
- move
- invalid move
- game over
- disconnect

---

## Frontend Rules

Keep UI simple and functional.

Priorities:

1. Chessboard
2. Correct interaction
3. Clear game status
4. Multiplayer functionality
5. Error handling
6. Basic responsive layout

Do NOT spend significant time on:
- animations
- elaborate gradients
- complicated themes
- authentication UI
- dashboards
- profiles
- leaderboards

---

## Single Player

Use chess.js for local chess rules.

The initial computer opponent may select a random legal move.

Do not implement sophisticated chess AI unless the entire MVP is already complete.

---

## Development Process

Implement one milestone at a time.

After every milestone:

1. Make the application runnable.
2. Test the feature manually.
3. Explain the architecture.
4. Do not start the next milestone until the current milestone works.

Do not rewrite working code unnecessarily.

Before adding a dependency, explain why it is required.

Before making architectural changes, explain the tradeoff.

---

## Code Quality

Prefer:

- small classes
- clear names
- straightforward control flow
- constructor dependency injection
- meaningful error messages
- minimal comments explaining WHY, not obvious WHAT

Avoid:
- overengineering
- giant classes
- duplicated business logic
- magic strings where constants/enums are appropriate
- frontend-only multiplayer validation