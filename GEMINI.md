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
  - `server/`: Server logic (NIO, Thread Pool).
    - `models/`: Private domain entities (User, Puzzle).
  - `client/`: CLI Client logic (TCP connection, UDP listener).
    - `engine/`: The core reactive hub (GameEngine, EngineState).
      - `modules/`: Specialized domain logic (User, Gameplay, History).
      - `state/`: Concrete engine states (LoggedOut, LoggedIn).
    - `ui/`: User Interface layer (InputProvider, ClientView).
    - `network/`: Network communication (TCP Gateway, UDP Service).
  - `common/`: Shared resources.
    - `models/`: Shared data structures (WordGroup, Category).
    - `dto/`: Data Transfer Objects used in both TCP and UDP.
    - `protocol/`: TCP Request/Response structure.
    - `notifications/`: UDP asynchronous message structure.

## Progress
- [x] Project initialization (Maven, directory structure).
- [x] `pom.xml` with dependencies (Gson, JUnit, Mockito).
- [x] Architectural Refactoring: Separation of Domain Models, DTOs, and Protocols.
- [x] Implement Type-Safe Generic Response system.
- [x] Implement Server configuration and Puzzle data loading.
- [x] Implement TCP request/response handling (NIO).
- [x] Implement User registration/login (Persistence).
- [x] Implement Puzzle logic (Session management).
- [x] Implement UDP notifications (New puzzle, Puzzle end summaries).
- [x] Implement CLI Client with input/output handlers.
- [x] Testing and validation (Comprehensive suite: 17/17 passed).
- [x] Optimization: High-performance Puzzle indexing (Offsets + Buffered I/O).
- [x] Refactoring Modulare: Separazione delle logiche Client (User, Gameplay, History) in moduli indipendenti.
- [x] Allineamento Specifiche (Allegato 1): Correzione dei nomi dei campi JSON (`name` vs `username`, `psw`) e ripristino dei nomi operazione originali (`requestGameInfo`, `requestLeaderboard`, etc.) per garantire la piena conformità.
- [x] **Semantic Terminology Refactoring**: Renamed all occurrences of "game" to "puzzle" when referring to a specific round/puzzle session, maintaining "game" only for the entire application.
- [x] **Protocol Compatibility**: Ensured that while Java names use "puzzle", JSON fields still use "gameId" via `@SerializedName` for strict compatibility with existing specifications.

## Latest Session Improvements (March 12, 2026)
- **Engine-Module Architecture**:
    - Renamed `com.connections.client.controller` -> `com.connections.client.engine`.
    - Renamed `com.connections.client.controller.adapters` -> `com.connections.client.engine.modules`.
    - Refactored `UserAdapter`, `GameplayAdapter`, and `HistoryAdapter` into `UserModule`, `GameplayModule`, and `HistoryModule`.
- **Architectural Clarity**: The naming now correctly identifies the `GameEngine` as the central reactive hub and the `Modules` as domain-specific logic providers rather than generic adapters.
- **Verification**: Full project recompilation and test suite execution (17/17 passed) confirmed zero regressions after the move.

## Recent Improvements
- **Clean Architecture**: Moved sensitive entities (`User`, `Game`) to server-private packages.
- **Protocol Type Safety**: Implemented `Response<T extends DTO>` to ensure only DTOs are sent over TCP.
- **Strict Spec Compliance (Allegato 1)**: Refactored `UserRequest`, `HistoryRequest` and `Operation` to match the exact JSON structure required by the project documentation, including:
    - Support for optional `playerName` and `topPlayers` in leaderboard requests.
    - Differentiation between `name` (registration) and `username` (login).
    - Use of exact operation strings required by the evaluation criteria.
- **Flexible Rules**: Refactored `WordGroup` and `GameRequest` to be agnostic of specific word counts, moving validation logic to the server.
- **Advanced Notifications**: Added `GameEndNotification` (global) and `PlayerGameSummaryNotification` (individual) via UDP.
- **Planned Modular Client**: Introduction of `ClientController` and three specialized modules:
    - `UserModule`: Authentication and account management.
    - `GameModule`: Active game interactions (proposals, current info/stats).
    - `HistoryModule`: Historical data, leaderboard, and global stats.
- **Contextual DTOs**: Refactoring `GameInfoDTO` and `GameStatsDTO` to distinguish explicitly between "Active" and "Concluded" game data as per project requirements.

## Communication Protocol (JSON)
- Requests: `{ "operation": "register", "name": "user", "psw": "pass" }`
- Responses: `{ "status": "ok", "message": "registered successfully", "data": { ... } }`
- Notifications: `{ "type": "new_game", ... }`
- **Behavioral Note**: Sending a `submitProposal` request results in a response containing a `GuessResultDTO` in the `data` field, informing the player about the outcome (points earned/lost and new total score).
