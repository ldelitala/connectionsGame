package com.connections.server.engine;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.server.ServerConfig;
import com.connections.server.network.UdpNotifier;
import com.connections.server.persistence.*;

import java.net.SocketException;

/**
 * The Brain of the server. Orchestrates business logic and coordinates domain cross-cutting
 * actions. Encapsulates coordinators, keeping persistence details internal to them.
 */
public class GameEngine {

    private final UserCoordinator userCoordinator;
    private final PuzzleCoordinator puzzleCoordinator;
    private final HistoryCoordinator historyCoordinator;
    private final UdpNotifier udpNotifier;

    public GameEngine(ServerConfig config) throws SocketException {
        this.udpNotifier = new UdpNotifier();

        UsersDAO usersDAO = new UsersDAO(config.getUserDataPath());
        PuzzlesDAO puzzlesDAO = new PuzzlesDAO(config.getPuzzleDataPath());
        HistoryDAO historyDAO = new HistoryDAO(config.getLogPath());
        ServerStateDAO stateDAO = new ServerStateDAO(config.getStatePath());

        // Resume puzzle rotation from last saved state
        int savedIndex = stateDAO.loadNextPuzzleIndex();
        puzzlesDAO.setNextPuzzleIndex(savedIndex);
        System.out.println("Resuming puzzle rotation from index: " + savedIndex);

        this.userCoordinator = new UserCoordinator(usersDAO, udpNotifier);
        this.historyCoordinator = new HistoryCoordinator(historyDAO, usersDAO, puzzlesDAO);

        // PuzzleCoordinator -> History notification bridge
        this.puzzleCoordinator = new PuzzleCoordinator(
                puzzlesDAO,
                userCoordinator,
                udpNotifier,
                stateDAO,
                config.getPuzzleDurationMillis(),
                config.getPuzzleDelayMillis(),
                (puzzleId, results) -> historyCoordinator.recordPuzzleResults(puzzleId, results));
    }
public void start() {
    puzzleCoordinator.startRotation();
}

public void stop() {
    System.out.println("Stopping Game Engine...");
    puzzleCoordinator.stop();
}

/**
...
     * Resolves business logic for any given request and operation.
     */
    public ServerResponse<?> resolve(ClientRequest req, Operation op, String username) {

        if (op.getRequestClass() == GameplayRequest.class) {
            return puzzleCoordinator.handle((GameplayRequest) req, op, username);
        }

        if (op.getRequestClass() == HistoryRequest.class) {
            return historyCoordinator.handle((HistoryRequest) req, op, username);
        }

        // 3. User & Session logic
        if (op.getRequestClass() == UserRequest.class) {

            return userCoordinator.handle((UserRequest) req, op, username);
        }

        return ServerResponse.error(op, "Invalid engine resolution.");
    }

    public void handleDisconnect(String username) {
        if (username != null) {
            userCoordinator.logout(username);
        }
    }
}
