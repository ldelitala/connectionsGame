# Connections Game - JSON Message Protocol

This document describes all JSON messages exchanged between the Client and the Server.

## 1. TCP Requests (Client -> Server)

All requests must contain an `"operation"` field.

### Register
```json
{
  "operation": "register",
  "username": "user123",
  "psw": "password123"
}
```

### Login
```json
{
  "operation": "login",
  "username": "user123",
  "psw": "password123",
  "udpPort": 12345
}
```
*Note: `udpPort` is optional but required to receive asynchronous notifications.*

### Logout
```json
{
  "operation": "logout"
}
```

### Update Credentials
```json
{
  "operation": "updateCredentials",
  "newName": "new_user123",
  "oldPsw": "old_password",
  "newPsw": "new_password"
}
```
*Note: Both `newName` and `newPsw` are optional (at least one should be present).*

### Submit Proposal
```json
{
  "operation": "submitProposal",
  "words": ["CAT", "DOG", "FISH", "BIRD"]
}
```

### Request Game Info (Personal Status)
```json
{
  "operation": "requestGameInfo",
  "gameId": 101
}
```
*Note: `gameId` is optional; if omitted, the server returns info for the current game.*

### Request Game Statistics (Global)
```json
{
  "operation": "requestGameStats",
  "gameId": 101
}
```
*Note: `gameId` is optional.*

### Request Player Statistics (Personal)
```json
{
  "operation": "requestPlayerStats"
}
```

### Request Leaderboard
**Option A: Top K players**
```json
{
  "operation": "requestLeaderboard",
  "topPlayers": 10
}
```
**Option B: Specific player rank**
```json
{
  "operation": "requestLeaderboard",
  "playerName": "user123"
}
```

---

## 2. TCP Responses (Server -> Client)

All responses follow a standard format:

```json
{
  "status": "ok",
  "message": "Human readable message",
  "data": { ... }
}
```
*In case of error, `status` will be `"error"` and `data` may be null.*

### Response Example: Request Game Info
```json
{
  "status": "ok",
  "message": "Game status for ID 1",
  "data": {
    "gameId": 1,
    "remainingTime": 245000,
    "foundGroups": [
      {
        "theme": "Animals",
        "words": ["CAT", "DOG", "FISH", "BIRD"]
      }
    ],
    "mistakes": 1,
    "currentScore": 2,
    "finished": false,
    "remainingWords": ["RED", "BLUE", "GREEN", "YELLOW", "LONDON", "PARIS", "ROME", "BERLIN", "PIZZA", "PASTA", "BURGER", "SUSHI"]
  }
}
```

### Response Example: Submit Proposal (Correct)
```json
{
  "status": "ok",
  "message": "Proposal processed.",
  "data": {
    "status": "ok",
    "message": "Correct! Group: Colors",
    "foundGroups": 2
  }
}
```

### Response Example: Player Stats
```json
{
  "status": "ok",
  "message": "Player stats.",
  "data": {
    "puzzlesCompleted": 5,
    "winRate": 80.0,
    "lossRate": 20.0,
    "currentStreak": 2,
    "maxStreak": 3,
    "perfectPuzzles": 1,
    "mistakeHistogram": [2, 1, 0, 1, 1],
    "totalScore": 54
  }
}
```

---

## 3. UDP Notifications (Server -> Client)

Sent asynchronously when a new game starts.

```json
{
  "type": "new_game",
  "gameId": 2,
  "words": ["WORD1", "WORD2", ..., "WORD16"]
}
```
