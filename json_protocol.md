# Connections Game - JSON Message Protocol

This document describes all JSON messages exchanged between the Client and the Server.

## 1. TCP Requests & Success Responses (status: "ok")

All requests must contain an `"operation"` field.

### Register
**Request** (Object: `UserRequest`)
```json
{
  "operation": "register",
  "name": "user123",
  "psw": "password"
}
```
**Response** (Object: `ServerResponse<DTO>`)
```json
{
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
**Response** (Object: `ServerResponse<DTO>`)
```json
{
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
**Response** (Object: `ServerResponse<DTO>`)
```json
{
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
*Note: `newName` or `newPsw` can be null if not being changed.*

**Response** (Object: `ServerResponse<DTO>`)
```json
{
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
*Note: Words are sent as a list. Server validates that length is exactly 4.*

**Response** (Object: `ServerResponse<GuessResultDTO>`)
```json
{
  "status": "ok",
  "data": {
    "status": "success",
    "pointsDelta": 6.0,
    "totalPoints": 18,
    "remainingWords": ["RED", "GREEN", "BLUE", "YELLOW", "LONDON", "PARIS", "ROME", "BERLIN", "PIZZA", "PASTA", "BURGER", "SUSHI"]
  }
}
```
*Note: pointsDelta can be negative for wrong guesses (status: "wrong").*

### Get Puzzle Info (requestGameInfo)
**Request** (Object: `GameplayRequest` or `HistoryRequest`)
```json
{ "operation": "requestGameInfo", "gameId": 123 }
```
*Note: gameId can be null or -1 for the current puzzle.*

**Response (Active Puzzle)** (Object: `ServerResponse<PuzzleStateDTO>`)
```json
{
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

**Response (Concluded Puzzle)** (Object: `ServerResponse<PuzzleStateDTO>`)
```json
{
  "status": "ok",
  "data": {
    "gameId": 123,
    "finished": true,
    "allGroups": [
        { "theme": "ANIMALS", "words": ["CAT", "DOG", "FISH", "BIRD"] },
        { "theme": "COLORS", "words": ["RED", "GREEN", "BLUE", "YELLOW"] },
        { "theme": "CITIES", "words": ["LONDON", "PARIS", "ROME", "BERLIN"] },
        { "theme": "FOODS", "words": ["PIZZA", "PASTA", "BURGER", "SUSHI"] }
    ],
    "correctGroupsCount": 4,
    "mistakes": 2,
    "score": 12
  }
}
```

### Get Puzzle Stats (requestGameStats)
**Request** (Object: `GameplayRequest` or `HistoryRequest`)
```json
{ "operation": "requestGameStats", "gameId": 123 }
```

**Response (Active Puzzle)** (Object: `ServerResponse<PuzzleStatsDTO>`)
```json
{
  "status": "ok",
  "data": {
    "gameId": 123,
    "finished": false,
    "remainingTime": 30000,
    "activePlayers": 10,
    "finishedPlayers": 2,
    "wonPlayers": 1
  }
}
```

**Response (Concluded Puzzle)** (Object: `ServerResponse<PuzzleStatsDTO>`)
```json
{
  "status": "ok",
  "data": {
    "gameId": 123,
    "finished": true,
    "totalParticipants": 25,
    "finishedPlayers": 20,
    "wonPlayers": 15,
    "averageScore": 8.5
  }
}
```

### Get Player Career Stats (requestPlayerStats)
**Request** (Object: `HistoryRequest`)
```json
{ "operation": "requestPlayerStats" }
```
**Response** (Object: `ServerResponse<PlayerStatsDTO>`)
```json
{
  "status": "ok",
  "data": {
    "puzzlesCompleted": 10,
    "winRate": 80.0,
    "totalScore": 150,
    "averageScore": 15.0
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
*Note: `playerName` and `topPlayers` are optional.*

**Response** (Object: `ServerResponse<LeaderboardDTO>`)
```json
{
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

### Error Response
Used when an operation fails due to invalid input, network issues, or internal errors.
(Object: `ServerResponse<DTO>`)
```json
{
  "status": "error",
  "message": "Invalid password."
}
```

### Forbidden Response
Used when a user tries to perform an action without proper authorization.
(Object: `ServerResponse<DTO>`)
```json
{
  "status": "forbidden",
  "message": "Authentication required."
}
```

---

## 3. UDP Notifications (Server -> Client)

Sent asynchronously via UDP.

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
Broadcast to all clients when a puzzle concluded.
(Object: `PuzzleEndNotification`)
```json
{
  "type": "game_end",
  "leaderboard": { 
    "entries": [
        { "rank": 1, "username": "user", "score": 100 }
    ] 
  },
  "stats": { 
    "gameId": 1, 
    "finished": true,
    "totalParticipants": 10,
    "finishedPlayers": 8,
    "wonPlayers": 5,
    "averageScore": 12.5
  }
}
```

### Personal Puzzle Summary
Sent privately to each participant when their puzzle (or the global puzzle) ends.
(Object: `PlayerPuzzleSummaryNotification`)
```json
{
  "type": "player_summary",
  "personalInfo": {
    "gameId": 1,
    "finished": true,
    "mistakes": 2,
    "score": 12,
    "foundGroups": [
        { "theme": "T1", "words": ["A", "B", "C", "D"] }
    ],
    "remainingWords": []
  }
}
```
