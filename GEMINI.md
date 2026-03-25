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
- `src/main/java/it/unipi/connections/`
  - `server/`: Decoupled Server logic.
    - `network/`: Infrastructure and I/O (`NioServer`, `ClientSession`, `UdpNotifier`).
    - `logic/`: Business logic coordinators (`UserCoordinator`, `PuzzleCoordinator`, `HistoryCoordinator`).
    - `persistence/`: Data Access Objects (`UsersDAO`, `PuzzlesDAO`, `HistoryDAO`).
    - `handler/`: Protocol bridge and worker pool management (`RequestDispatcher`).
    - `models/`: Private domain entities (`User`, `Puzzle`, `PlayerResult`).
  - `client/`: CLI Client logic (TCP connection, UDP listener).
    - `engine/`: The core reactive hub (GameEngine, EngineState).
      - `modules/`: Specialized domain logic (User, Gameplay, History).
      - `state/`: Concrete engine states (LoggedOut, LoggedIn).
    - `ui/`: User Interface layer (InputProvider, ClientView).
    - `network/`: Network communication (TCP Gateway, UDP Service).
  - `common/`: Shared resources.
    - `models/`: Shared data structures (WordGroup, Category).
    - `protocol/`: TCP Request/Response structure.
      - `server_responses/`: Logic for responses from server.
        - `dto/`: Data Transfer Objects used in both TCP and UDP.
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
- [x] Testing and validation (Comprehensive suite: 16/16 passed).
- [x] Optimization: High-performance Puzzle indexing (Offsets + Buffered I/O).
- [x] Refactoring Modulare: Separazione delle logiche Client (User, Gameplay, History) in moduli indipendenti.
- [x] Allineamento Specifiche (Allegato 1): Correzione dei nomi dei campi JSON (`name` vs `username`, `psw`) e ripristino dei nomi operazione originali (`requestGameInfo`, `requestLeaderboard`, etc.) per garantire la piena conformità.
- [x] **Semantic Terminology Refactoring**: Renamed all occurrences of "game" to "puzzle" when referring to a specific round/puzzle session, maintaining "game" only for the entire application.
- [x] **Protocol Compatibility**: Ensured that while Java names use "puzzle", JSON fields still use "gameId" via `@SerializedName` for strict compatibility with existing specifications.
- [x] **Batch Refactoring (DTOs)**: Moved DTO classes to `common.protocol.server_responses.dto` for better architectural grouping.
- [x] **Server Core Rewrite (NIO 2.0)**: Completely refactored the server into a decoupled, thread-safe architecture with a dedicated Worker Pool and logic Coordinators.

## Latest Session Improvements (March 12, 2026)
- **Decoupled Server Architecture**:
    - **Persistence Layer**: Optimized `HistoryDAO` and `PuzzlesDAO` using file offsets to minimize RAM usage. `PuzzlesDAO` now uses a high-performance `PushbackInputStream` for byte-level parsing.
    - **Performance Benchmarking**: Verified that `PuzzlesDAO` can index ~900 puzzles in 30ms, with an estimated throughput of 14-20 MB/s (approx. 74s for a 1GB file).
    - **Logic Layer**: Introduced `UserCoordinator`, `PuzzleCoordinator`, and `HistoryCoordinator` to isolate business logic from network handling.
    - **NIO Core**: Implemented a robust `NioServer` with a `Selector` loop that offloads JSON processing to a fixed thread pool via `RequestDispatcher`.
- **Terminology Alignment**: Finalized "game" (app) vs "puzzle" (round) terminology across all layers.
- **DTO Batch Refactoring**:
    - Moved all DTO files from `it.unipi.connections.common.dto` to `it.unipi.connections.common.protocol.server_responses.dto`.
- **Testing & Quality**:
    - Reorganized test suite into specialized packages (`logic`, `persistence`).
    - Added `PuzzlesDAOPerformanceTest` for automated bottleneck detection.

## Recent Improvements
- **Clean Architecture**: Moved sensitive entities (`User`, `Game`) to server-private packages.
- **Protocol Type Safety**: Implemented `Response<T extends DTO>` to ensure only DTOs are sent over TCP.
- **Strict Spec Compliance (Allegato 1)**: Refactored `UserRequest`, `HistoryRequest` and `Operation` to match the exact JSON structure required by the project documentation.
- **Advanced Notifications**: Added `PuzzleEndNotification` (global) and `PlayerPuzzleSummaryNotification` (individual) via UDP.

## Communication Protocol (JSON)
- Requests: `{ "operation": "register", "name": "user", "psw": "pass" }`
- Responses: `{ "status": "ok", "message": "registered successfully", "data": { ... } }`
- Notifications: `{ "type": "new_game", ... }`
- **Behavioral Note**: Sending a `submitProposal` request results in a response containing a `GuessResultDTO` in the `data` field, informing the player about the outcome.
