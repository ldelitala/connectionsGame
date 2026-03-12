# Connections Game - JSON Message Protocol

This document describes all JSON messages exchanged between the Client and the Server.

## 1. TCP Requests (Client -> Server)

All requests must contain an `"operation"` field.

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
Returns an updated **Active Game** state (see "Get Game Info" response format below) in the `data` field. This allows the client to immediately render the updated board.

### Get Game Info
**Request**
```json
{ "operation": "getGameInfo", "gameId": 123 }
```
*Note: gameId can be null or -1 for the current game.*

**Response (Active Game)**
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

**Response (Concluded Game)**
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

### Get Game Stats
**Request**
```json
{ "operation": "getGameStats", "gameId": 123 }
```

**Response (Active Game)**
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

**Response (Concluded Game)**
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

### Get Leaderboard
```json
{ "operation": "getLeaderboard", "topPlayers": 10 }
```

---

## 2. TCP Responses (Server -> Client)

All responses follow a standard generic format:

```json
{
  "status": "ok",
  "message": "Human readable message",
  "data": { ... }
}
```

### Response: Leaderboard / Rank
When requesting a rank for a specific player, the `data` field contains a single entry:
```json
{
  "status": "ok",
  "message": "Rank.",
  "data": {
    "rank": 5,
    "username": "user123",
    "score": 120
  }
}
```

---

## 3. UDP Notifications (Server -> Client)

Sent asynchronously via UDP.

### New Game Started
```json
{
  "type": "new_game",
  "gameId": 2,
  "words": ["WORD1", "WORD2", ..., "WORD16"]
}
```

### Game Ended (Global Summary)
Broadcast to all clients when a game concluded.
```json
{
  "type": "game_end",
  "leaderboard": { "entries": [...] },
  "stats": { "gameId": 1, "winnersCount": 5, ... }
}
```

### Personal Game Summary
Sent privately to each participant when their game (or the global game) ends.
```json
{
  "type": "player_summary",
  "personalInfo": {
    "gameId": 1,
    "mistakes": 2,
    "score": 12,
    "finished": true,
    "won": true,
    "foundGroups": [...],
    "remainingWords": []
  }
}
```
