package it.unipi.connections.client.engine;

import it.unipi.connections.client.network.NetworkGateway;
import it.unipi.connections.client.engine.state.*;
import it.unipi.connections.client.ui.ClientView;
import it.unipi.connections.client.ui.InputProvider;
import it.unipi.connections.common.protocol.Operation;
import it.unipi.connections.common.protocol.client_requests.ClientRequest;
import it.unipi.connections.common.protocol.server_responses.ServerResponse;
import it.unipi.connections.common.protocol.server_responses.Status;
import it.unipi.connections.common.protocol.server_responses.dto.DTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * The Game Engine: The central reactive hub of the application.
 */
public class GameEngine implements InputProvider.InputListener {

    private final NetworkGateway gateway;
    private final int udpPort;
    private final ClientView view;
    private final InputProvider inputProvider;
    private final Gson gson = new Gson();

    private EngineState currentState;

    public GameEngine(NetworkGateway gateway, int udpPort, ClientView view,
            InputProvider inputProvider) {
        this.gateway = gateway;
        this.udpPort = udpPort;
        this.view = view;
        this.inputProvider = inputProvider;
        this.currentState = new LoggedOutState(this);
    }

    public void mainLoop() {
        view.renderPrompt(currentState.getPrompt());
        inputProvider.startListening(this);
        // This will loop until the provider signals to stop (e.g., onExit)
    }

    // --- Action Implementations (Delegating to State) ---

    @Override
    public void onRegister(String username, String password) {
        currentState.onRegister(username, password);
        postAction();
    }

    @Override
    public void onLogin(String username, String password) {
        currentState.onLogin(username, password);
        postAction();
    }

    @Override
    public void onLogout() {
        currentState.onLogout();
        postAction();
    }

    @Override
    public void onUpdateCredentials(String oldPass, String newName, String newPass) {
        currentState.onUpdateCredentials(oldPass, newName, newPass);
        postAction();
    }

    @Override
    public void onGuess(List<String> words) {
        currentState.onGuess(words);
        postAction();
    }

    @Override
    public void onRequestBoard() {
        currentState.onRequestBoard();
        postAction();
    }

    @Override
    public void onRequestStats() {
        currentState.onRequestStats();
        postAction();
    }

    @Override
    public void onRequestPastInfo(int puzzleId) {
        currentState.onRequestPastInfo(puzzleId);
        postAction();
    }

    @Override
    public void onRequestGlobalStats(int puzzleId) {
        currentState.onRequestGlobalStats(puzzleId);
        postAction();
    }

    @Override
    public void onRequestRanking(String username, Integer topK) {
        currentState.onRequestRanking(username, topK);
        postAction();
    }

    @Override
    public void onRequestHistory() {
        currentState.onRequestHistory();
        postAction();
    }

    @Override
    public void onExit() {
        view.showMessage("Exiting Engine...");
        currentState.onExit();
        inputProvider.stop();
    }

    // other system-level actions hendled by the engine itself (e.g., help, error
    // display) can be
    // implemented here directly
    @Override
    public void onHelp() {
        view.showHelp(currentState instanceof LoggedInState);
        postAction();
    }

    @Override
    public void onError(String message) {
        view.showError(message);
        if (message.contains("Input scanner") || message.contains("Fatal input error")) {
            onExit();
        }
    }

    // --- Helper Methods ---
    private void postAction() {
        view.renderPrompt(currentState.getPrompt());
    }

    public void changeState(EngineState newState) {
        this.currentState = newState;
    }

    /**
     * Executes a network request synchronously, handles the server response, and
     * automatically
     * triggers UI rendering.
     */
    public Status execute(ClientRequest request) {
        if (request == null || !request.isValid()) {
            view.showError("Invalid request. Please check the required parameters.");
            return Status.ERROR;
        }

        try {
            String jsonReq = gson.toJson(request);
            String jsonRes = gateway.sendRequest(jsonReq);

            Operation op = request.getOperation();
            Class<? extends DTO> dtoClass = op.getResponseDtoClass();

            Type type = TypeToken.get(ServerResponse.class).getType();
            if (dtoClass != null) {
                type = TypeToken.getParameterized(ServerResponse.class, dtoClass).getType();
            }

            ServerResponse<? extends DTO> response = gson.fromJson(jsonRes, type);
            view.showResponse(response);

            if (response.getStatus() == Status.OK && response.getData() != null) {
                view.render(response.getData());
            }

            return response.getStatus();

        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
            return Status.ERROR;
        } catch (Exception e) {
            view.showError("Unexpected error during request processing: " + e.getMessage());
            return Status.ERROR;
        }
    }

    // --- Getters for states ---
    public NetworkGateway getGateway() {
        return gateway;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public ClientView getView() {
        return view;
    }
}
