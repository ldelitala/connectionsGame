# Connections: Collega le parole e vinci!

Project for the "Reti e Laboratorio III" course (A.A. 2025/26) at University of Pisa.

## Project Structure
- `src/main/java/it/unipi/connections/server`: Server-side logic (NIO, Thread Pool).
- `src/main/java/it/unipi/connections/client`: Client-side logic (CLI).
- `src/main/java/it/unipi/connections/common`: Shared models, DTOs, and protocols.
- `config/`: Application configuration files.
- `data/`: Persistence files for user data and game logs.

## Building and Running
### With Maven
To compile the project:
```bash
mvn clean compile
```
To run tests:
```bash
mvn test
```
To run the server:
```bash
mvn exec:java -Dexec.mainClass="it.unipi.connections.server.ServerMain"
```
To run the client:
```bash
mvn exec:java -Dexec.mainClass="it.unipi.connections.client.ClientMain"
```

## Features
- [x] User registration and login (TCP).
- [x] Word grouping game (Connections logic).
- [x] Multithreaded server with NIO (Non-blocking I/O).
- [x] Persistence of user accounts and statistics.
- [x] Asynchronous notifications via UDP (New games, final summaries).
- [x] Global leaderboard and personal stats.
- [x] Type-safe communication protocol with shared DTOs.
