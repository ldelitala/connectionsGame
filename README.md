# Connections: Collega le parole e vinci!

Project for the "Reti e Laboratorio III" course (A.A. 2025/26) at University of Pisa.

## Project Structure
- `src/main/java/com/connections/server`: Server-side logic.
- `src/main/java/com/connections/client`: Client-side logic (CLI).
- `src/main/java/com/connections/common`: Shared models, constants, and utilities.
- `config/`: Application configuration files (JSON).
- `data/`: Persistence files for user data and game logs.
- `libs/`: External JAR dependencies.

## Building and Running
### With Maven
To compile the project:
```bash
mvn compile
```
To run the server:
```bash
mvn exec:java -Dexec.mainClass="com.connections.server.ServerMain"
```
To run the client:
```bash
mvn exec:java -Dexec.mainClass="com.connections.client.ClientMain"
```

### With `javac` (Manual)
To compile manually (assuming Gson is in `libs/`):
```bash
javac -cp "libs/*:src/main/java" src/main/java/com/connections/server/ServerMain.java -d target/classes
```

## Features
- [ ] User registration and login (TCP).
- [ ] Word grouping game (Connections).
- [ ] Multithreaded server with NIO.
- [ ] Persistence of user and game data.
- [ ] Asynchronous notifications via UDP.
- [ ] Periodic user stats and leaderboard.
