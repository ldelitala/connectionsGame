# Connections Game - JSON Message Protocol

This document describes all JSON messages exchanged between the Client and the Server.

## 1. TCP Requests & Success Responses (status: "ok")

All requests must contain an `"operation"` field.

### Register
**Request**
```json
{
  "operation": "register",
  "name": "user123",
  "psw": "password"
}
```
**Response**
```json
{
  "status": "ok",
  "message": "User registered successfully."
}
```

### Login
**Request**
```json
{
  "operation": "login",
  "username": "user123",
  "psw": "password",
  "udpPort": 8081
}
```
**Response**
```json
{
  "status": "ok",
  "message": "Login successful."
}
```

### Logout
**Request**
```json
{
  "operation": "logout"
}
```
**Response**
```json
{
  "status": "ok",
  "message": "Logout successful."
}
```

### Update Credentials
**Request**
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

**Response**
```json
{
  "status": "ok",
  "message": "Credentials updated successfully."
}
```

### Submit Proposal
**Request**
```json
{
  "operation": "submitProposal",
  "words": ["CAT", "DOG", "FISH", "BIRD"]
}
```
*Note: Words are sent as a list. Server validates that length is exactly 4.*

**Response**
```json
{
  "status": "ok",
  "data": {
    "status": "success",
    "pointsDelta": 6.0,
    "totalPoints": 18
  }
}
```
*Note: pointsDelta can be negative for wrong guesses (status: "wrong").*

### Get Puzzle Info (requestGameInfo)
**Request**
```json
{ "operation": "requestGameInfo", "gameId": 123 }
```
*Note: gameId can be null or -1 for the current puzzle.*

**Response (Active Puzzle)**
```json
{
  "status": "ok",
  "data": {
    "gameId": 123,
    "finished": false,
    "remainingTime": 30000,
    "foundGroups": [...],
    "remainingWords": ["W1", "W2", ...],
    "mistakes": 1,
    "score": 4
  }
}
```

**Response (Concluded Puzzle)**
```json
{
  "status": "ok",
  "data": {
    "gameId": 123,
    "finished": true,
    "allGroups": [...],
    "correctGroupsCount": 4,
    "mistakes": 2,
    "score": 12
  }
}
```

### Get Puzzle Stats (requestGameStats)
**Request**
```json
{ "operation": "requestGameStats", "gameId": 123 }
```

**Response (Active Puzzle)**
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

**Response (Concluded Puzzle)**
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
**Request**
```json
{ "operation": "requestPlayerStats" }
```
**Response**
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
**Request**
```json
{ 
  "operation": "requestLeaderboard", 
  "playerName": "user123", 
  "topPlayers": 10 
}
```
*Note: `playerName` and `topPlayers` are optional.*

**Response**
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
```json
{
  "status": "error",
  "message": "Invalid password."
}
```

### Forbidden Response
Used when a user tries to perform an action without proper authorization (e.g., requesting stats for a puzzle before logging in).
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
```json
{
  "type": "new_game",
  "gameId": 2,
  "words": ["WORD1", "WORD2", ..., "WORD16"]
}
```

### Puzzle Ended (Global Summary)
Broadcast to all clients when a puzzle concluded.
```json
{
  "type": "game_end",
  "leaderboard": { "entries": [...] },
  "stats": { "gameId": 1, "winnersCount": 5, ... }
}
```

### Personal Puzzle Summary
Sent privately to each participant when their puzzle (or the global puzzle) ends.
```json
{
  "type": "player_summary",
  "personalInfo": {
    "gameId": 1,
    "mistakes": 2,
    "score": 12,
    "finished": true,
    "foundGroups": [...],
    "remainingWords": []
  }
}
```
