# Connections: Project Progress & Specs

## Technical Requirements
- **Language**: Java 8
- **Protocol**: 
  - **TCP**: For requests (Registration, Login, Proposals, Stats) - JSON format.
  - **UDP**: For asynchronous notifications (Game start/end, leaderboard updates).
  - **NIO**: Server must use Java NIO for TCP connections.
  - **Multithreading**: Server must use a thread pool.
- **Data Persistence**:
  - `Connections_Data.json`: Source for game data.
  - `users.json`: Persistent storage for user accounts and statistics.
  - `games_log.json`: History of games played.

## Project Structure
- `src/main/java/com/connections/`
  - `server/`: Server logic (NIO, Thread Pool, UDP Notifications).
  - `client/`: CLI Client logic (TCP connection, UDP listener).
  - `common/`: Shared models (User, Game, WordGroup, Message).
  - `utils/`: Networking and JSON utilities.

## Progress
- [x] Project initialization (Maven, directory structure).
- [x] `pom.xml` with dependencies (Gson, JUnit).
- [x] Basic `ServerMain` and `ClientMain` skeletons.
- [x] Define shared models (`common` package).
- [x] Implement Server configuration and Game data loading.
- [x] Implement TCP request/response handling (NIO).
- [x] Implement User registration/login (Persistence).
- [x] Implement Game logic (Session management).
- [x] Implement UDP notifications.
- [x] Implement CLI Client with input/output handlers.
- [x] Testing and validation (Comprehensive suite: 17/17 passed).
- [x] Optimization: High-performance Game indexing (Offsets + Buffered I/O).

## Recent Improvements
- **GamesDAO Refactoring**: Migrated from low-level manual parsing to high-level JSON management while retaining byte-offset efficiency.
- **Indexing Optimization**: Implemented `BufferedInputStream` and Regex-based ID extraction for rapid indexing of multi-GB files (~200k games/sec).
- **Stability**: Fixed compilation issues in `ClientMain` and `ConnectionsServer`, ensuring a clean build and full test coverage.

## Communication Protocol (JSON)
- Requests: `{ "operation": "register", "name": "user", "psw": "pass" }`
- Responses: `{ "status": "ok", "message": "registered successfully" }` (or errors).
