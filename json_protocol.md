# Connections Game - JSON Message Protocol

This document describes all JSON messages exchanged between the Client and the Server.

## 1. TCP Requests & Success Responses (status: "ok")

All requests must contain an `"operation"` field. All responses now include the corresponding `"operation"` field for self-description.

### Register
**Request** (Object: `UserRequest`)
```json
{
  "operation": "register",
  "name": "user123",
  "psw": "password"
}
```
**Response** (Object: `UserResponse`)
```json
{
  "operation": "register",
  "status": "ok",
  "message": "User registered successfully."
}
```

### Login
**Request** (Object: `UserRequest`)
```json
{
  "operation": "login",
  "username": "user123",
  "psw": "password",
  "udpPort": 8081
}
```
**Response** (Object: `UserResponse`)
```json
{
  "operation": "login",
  "status": "ok",
  "message": "Login successful."
}
```

### Logout
**Request** (Object: `UserRequest`)
```json
{
  "operation": "logout"
}
```
**Response** (Object: `UserResponse`)
```json
{
  "operation": "logout",
  "status": "ok",
  "message": "Logout successful."
}
```

### Update Credentials
**Request** (Object: `UserRequest`)
```json
{
  "operation": "updateCredentials",
  "oldName": "user123",
  "oldPsw": "password",
  "newName": "newUser",
  "newPsw": "newPassword"
}
```
**Response** (Object: `UserResponse`)
```json
{
  "operation": "updateCredentials",
  "status": "ok",
  "message": "Credentials updated successfully."
}
```

### Submit Proposal
**Request** (Object: `GameplayRequest`)
```json
{
  "operation": "submitProposal",
  "words": ["CAT", "DOG", "FISH", "BIRD"]
}
```
**Response** (Object: `GameplayResponse<GuessResultDTO>`)
```json
{
  "operation": "submitProposal",
  "status": "ok",
  "data": {
    "status": "success",
    "pointsDelta": 6.0,
    "totalPoints": 18,
    "remainingWords": ["RED", "GREEN", "BLUE", "YELLOW", "LONDON", "PARIS", "ROME", "BERLIN", "PIZZA", "PASTA", "BURGER", "SUSHI"]
  }
}
```

### Get Puzzle Info (requestGameInfo)
**Request** (Object: `GameplayRequest` or `HistoryRequest`)
```json
{ "operation": "requestGameInfo", "gameId": 123 }
```
*Note: server uses `gameId` to distinguish between active puzzle (null/missing) and history record.*

**Response** (Object: `GameplayResponse<PuzzleStateDTO>` or `HistoryResponse<PuzzleStateDTO>`)
```json
{
  "operation": "requestGameInfo",
  "status": "ok",
  "data": {
    "gameId": 123,
    "finished": false,
    "remainingTime": 30000,
    "foundGroups": [
        { "theme": "ANIMALS", "words": ["CAT", "DOG", "FISH", "BIRD"] }
    ],
    "remainingWords": ["RED", "GREEN", ...],
    "mistakes": 1,
    "score": 4
  }
}
```

### Get Puzzle Stats (requestGameStats)
**Request** (Object: `GameplayRequest` or `HistoryRequest`)
```json
{ "operation": "requestGameStats", "gameId": 123 }
```

**Response** (Object: `GameplayResponse<PuzzleStatsDTO>` or `HistoryResponse<PuzzleStatsDTO>`)
```json
{
  "operation": "requestGameStats",
  "status": "ok",
  "data": {
    "gameId": 123,
    "finished": false,
    "activePlayers": 10,
    "finishedPlayers": 2,
    "wonPlayers": 1
  }
}
```

### Get Player Career Stats (requestPlayerStats)
**Request** (Object: `HistoryRequest`)
```json
{ "operation": "requestPlayerStats" }
```
**Response** (Object: `HistoryResponse<PlayerStatsDTO>`)
```json
{
  "operation": "requestPlayerStats",
  "status": "ok",
  "data": {
    "puzzlesCompleted": 10,
    "winRate": 80.0,
    "totalScore": 150
  }
}
```

### Get Leaderboard (requestLeaderboard)
**Request** (Object: `HistoryRequest`)
```json
{ 
  "operation": "requestLeaderboard", 
  "playerName": "user123", 
  "topPlayers": 10 
}
```
**Response** (Object: `HistoryResponse<LeaderboardDTO>`)
```json
{
  "operation": "requestLeaderboard",
  "status": "ok",
  "data": {
    "entries": [
      { "rank": 1, "username": "pro", "score": 500 },
      { "rank": 2, "username": "user123", "score": 150 }
    ]
  }
}
```

---

## 2. Other Server Responses (error & forbidden)

All error/forbidden responses now include the relevant `operation` field.

### Error Response
(Object: `UserResponse`, `GameplayResponse` or `HistoryResponse`)
```json
{
  "operation": "submitProposal",
  "status": "error",
  "message": "Invalid password."
}
```

### Forbidden Response
(Object: `UserResponse`, `GameplayResponse` or `HistoryResponse`)
```json
{
  "operation": "requestPlayerStats",
  "status": "forbidden",
  "message": "Authentication required."
}
```

---

## 3. UDP Notifications (Server -> Client)

Sent asynchronously via UDP. No changes to the structure.

### New Puzzle Started
(Object: `NewPuzzleNotification`)
```json
{
  "type": "new_game",
  "gameId": 2,
  "words": ["WORD1", "WORD2", ..., "WORD16"]
}
```

### Puzzle Ended (Global Summary)
(Object: `PuzzleEndNotification`)
```json
{
  "type": "game_end",
  "leaderboard": { ... },
  "stats": { ... }
}
```

### Personal Puzzle Summary
(Object: `PlayerPuzzleSummaryNotification`)
```json
{
  "type": "player_summary",
  "personalInfo": { ... }
}
```
