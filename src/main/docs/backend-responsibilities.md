
# MoodMatch Backend – Responsibilities

## 1. Overview

The MoodMatch backend is a Ktor-based server that provides:
- Anonymous but stable identity for each device/user.
- Real-time random matching between two users based on mood.
- WebSocket-based chat once users are matched.
- Basic security and observability for production use.

Clients:
- Android app (Ktor HttpClient + WebSockets).
- Future: iOS app, web client, etc.

The backend exposes:
- HTTP endpoints for slower/one-off operations (e.g., registration, health).
- A WebSocket endpoint for real-time matchmaking and chat.

---

## 2. Core Responsibilities

### 2.1 Identity & Authentication

- Generate a unique `userId` for each new user/device.
- Issue an `authToken` (opaque token or JWT) for that user.
- Validate tokens on every HTTP and WebSocket request.
- Derive the effective `userId` from the token, not from client payload.
- Store basic user profile:
    - `userId`, `nickname`, `mood`, `avatar`
    - `createdAt`, `lastSeenAt`
    - `status` (`offline`, `idle`, `in_queue`, `in_room`)

### 2.2 Presence & Connection Management

- Track which `userId` is connected via which WebSocket connection.
- Maintain mapping:
    - `userId -> connection`
    - `connection -> userId`
- Update user `status` based on connection and matchmaking state.
- Handle disconnects:
    - If user was in queue → remove from queue.
    - If user was in room → end room and notify partner.

### 2.3 Matchmaking Queues

- Maintain in-memory queues of users waiting to be matched, grouped by mood.
    - Example: `queue["chill"]`, `queue["fire"]`, etc.
- When a user sends `JOIN_QUEUE`:
    - Validate they are authenticated and not in a room.
    - Set status to `in_queue`.
    - Add them to the appropriate queue.
- When enough users are in a queue:
    - Select a compatible pair (FIFO or random).
    - Create a `roomId` and a Room record.
    - Mark both users as `in_room`.
    - Notify both with `MATCH_FOUND` (room + partner info).

### 2.4 Room & Chat Lifecycle

- Maintain in-memory record of active rooms:
    - `roomId`, `userAId`, `userBId`, `moodForMatch`, `startedAt`, `endedAt`, `status`.
- Keep mapping `userId -> currentRoomId` if in a room.
- On `CHAT` messages:
    - Validate the sender belongs to the given `roomId`.
    - Optionally store message metadata (for debugging later).
    - Forward the message to the other participant’s WebSocket connection.
- Handle:
    - `LEAVE_ROOM` from client.
    - Unexpected disconnects.
    - Notify partner via `PARTNER_LEFT`.
    - Mark room as `ended`, set `endedAt`.

### 2.5 Security & Validation

- Transport security:
    - All external traffic must go over HTTPS/WSS (TLS).
    - HTTP → redirect to HTTPS (handled by hosting/proxy).
- Application-level security:
    - Require valid `authToken` for all state-changing operations.
    - Never trust client-provided `userId` without verifying it against token.
    - Validate payloads:
        - Max nickname length.
        - Max message length.
        - Valid `mood` values, etc.
    - Provide `ERROR` messages with codes for invalid requests.
- Rate limiting (basic):
    - Limit frequency of:
        - `JOIN_QUEUE` / `LEAVE_QUEUE`.
        - `CHAT` messages (to avoid spam/abuse).

### 2.6 Observability & Health

- Expose a lightweight health endpoint:
    - `GET /api/health` → ready/live status + app env + version.
- Optionally expose a secured admin stats endpoint:
    - `GET /api/admin/stats` → queue sizes, active rooms, online users.
- Log important events:
    - Server start/stop.
    - Auth errors.
    - Room creation/end.
    - Significant errors/exception traces (without sensitive data).
- Enable basic metrics (at least conceptually):
    - Number of rooms created.
    - Number of active rooms.
    - Queue sizes per mood.
    - Messages sent.

---

## 3. Data & In-Memory State Model

The backend will maintain the following logical entities (in memory for v1):

### 3.1 User

- `userId: String`
- `nickname: String`
- `mood: String` (e.g. `"chill"`, `"fire"`, `"silly"`, `"shy"`, `"random"`)
- `avatar: String` (emoji/animal)
- `status: String` (`offline` | `idle` | `in_queue` | `in_room`)
- `createdAt: Long`
- `lastSeenAt: Long`

### 3.2 Connection / Presence

- `userId: String`
- `connectionId` or a server-side connection handle
- `currentRoomId: String?`
- `lastHeartbeatAt: Long` (optional, for ping/pong)

### 3.3 Room

- `roomId: String`
- `userAId: String`
- `userBId: String`
- `moodForMatch: String`
- `status: String` (`active` | `ended`)
- `startedAt: Long`
- `endedAt: Long?`

### 3.4 Queue

- `queuesByMood: Map<String, List<UserInQueue>>`
- `UserInQueue` includes:
    - `userId: String`
    - `joinedAt: Long`
    - `preferredMode: String` (e.g., `same_mood` or `random`)

No persistent database is required for v1; all of this can be in memory.  
Later versions may persist Users, Rooms, and Messages to a database.

---

## 4. External Interfaces (High-Level)

### 4.1 HTTP

- `POST /api/users/registerOrUpdate`
    - Input: nickname, mood, avatar.
    - Output: `userId`, `authToken`, full profile.
- `GET /api/health`
    - Basic health info.

(Future endpoints may include history, feedback, etc.)

### 4.2 WebSocket

- Endpoint: `/ws/chat`
- Message envelope:
    - `type: String`
    - `payload: Object`
- Main message types:
    - `AUTH`, `AUTH_OK`, `AUTH_ERROR`
    - `JOIN_QUEUE`, `LEAVE_QUEUE`, `MATCH_FOUND`, `QUEUE_WAITING`, `MATCH_CANCELLED`
    - `CHAT`, `TYPING`, `PARTNER_LEFT`
    - `ERROR`

---

## 5. Non-Goals for v1

- No long-term message history or user accounts/passwords.
- No social features (friends, blocks, reporting) yet.
- No multi-room support per user (1 room max at a time).

These can be added in future iterations once the core loop (onboard → match → chat → leave) is stable.
