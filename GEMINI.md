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
    - `models/`: Private domain entities (User, Game).
  - `client/`: CLI Client logic (TCP connection, UDP listener).
  - `common/`: Shared resources.
    - `models/`: Shared data structures (WordGroup).
    - `dto/`: Data Transfer Objects used in both TCP and UDP.
    - `protocol/`: TCP Request/Response structure.
    - `notifications/`: UDP asynchronous message structure.

## Progress
- [x] Project initialization (Maven, directory structure).
- [x] `pom.xml` with dependencies (Gson, JUnit, Mockito).
- [x] Architectural Refactoring: Separation of Domain Models, DTOs, and Protocols.
- [x] Implement Type-Safe Generic Response system.
- [x] Implement Server configuration and Game data loading.
- [x] Implement TCP request/response handling (NIO).
- [x] Implement User registration/login (Persistence).
- [x] Implement Game logic (Session management).
- [x] Implement UDP notifications (New game, Game end summaries).
- [x] Implement CLI Client with input/output handlers.
- [x] Testing and validation (Comprehensive suite: 17/17 passed).
- [x] Optimization: High-performance Game indexing (Offsets + Buffered I/O).
- [x] Refactoring Modulare: Separazione delle logiche Client (User, Game, History) in moduli indipendenti.
- [x] Raffinamento Protocollo: Specializzazione di GameInfo/Stats DTO per aderenza rigorosa alle specifiche (Corrente vs Passato).
- [x] Allineamento Specifiche (Allegato 1): Correzione dei nomi dei campi JSON (`name` vs `username`, `psw`) e ripristino dei nomi operazione originali (`requestGameInfo`, `requestLeaderboard`, etc.) per garantire la piena conformità.

## Latest Session Improvements (March 10, 2026)
- **Global Localization**: Standardized the entire project to **English** for all user-facing outputs, logs, and error messages.
- **Clean Code & Magic Values**: Extracted all hardcoded logical values (commands, scoring rules, timeouts) into `private static final` constants. Organized them by category (Commands, Scoring, Config).
- **Security Fixes**: Updated `UserModuleView` to strictly require the old password for credential updates, aligning the client with the server protocol.
- **Auto-Logout on Exit**: Implemented a graceful exit strategy where the client attempts to notify the server (logout) before terminating the process.
- **Architectural Cleanup**:
    - Centralized System-level commands (`help`, `exit`) in the `Controller`.
    - Renamed the `state` package to `controller_state` for better role clarity.
    - Added comprehensive **Javadoc** (English) to the core client logic.
- **Tooling optimization**: Configured `eclipse-formatter.xml` and used `@formatter` tags to preserve manual alignment of multi-line string constants (CLI help blocks).

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
