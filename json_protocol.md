# Connections Game - JSON Message Protocol

This document describes all JSON messages exchanged between the Client and the Server.

## 1. TCP Requests (Client -> Server)

All requests must contain an `"operation"` field.

### Submit Proposal
```json
{
  "operation": "submitProposal",
  "words": ["CAT", "DOG", "FISH", "BIRD"]
}
```
*Note: Words are sent as a list. Server validates that length is exactly 4.*

### Request Leaderboard
**Option A: Top K players**
```json
{ "operation": "requestLeaderboard", "topPlayers": 10 }
```
**Option B: Specific player rank**
```json
{ "operation": "requestLeaderboard", "playerName": "user123" }
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
