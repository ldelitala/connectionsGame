package com.connections.client.engine;

import com.connections.client.network.NetworkGateway;
import com.connections.client.ui.ClientView;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.Operation;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;
import com.connections.common.protocol.server_responses.dto.DTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Braccio operativo del GameEngine per le comunicazioni di rete.
 * Centralizza la serializzazione, l'invio e la deserializzazione automatica
 * basata sul contratto definito nell'enum Operation.
 */
public class EngineExecutor {

    private final NetworkGateway gateway;
    private final ClientView view;
    private final Gson gson = new Gson();

    public EngineExecutor(NetworkGateway gateway, ClientView view) {
        this.gateway = gateway;
        this.view = view;
    }

    /**
     * Esegue una richiesta in modo sincrono, gestisce la risposta e aggiorna la View.
     * 
     * @param request La richiesta da inviare.
     * @return Lo stato della risposta (OK, ERROR, ecc.) per eventuali logiche di controllo nel chiamante.
     */
    public Status execute(ClientRequest request) {
        try {
            // 1. Serializzazione
            String jsonReq = gson.toJson(request);
            
            // 2. Invio e attesa risposta
            String jsonRes = gateway.sendRequest(jsonReq);
            
            // 3. Deserializzazione dinamica
            Operation op = request.getOperation();
            Class<? extends DTO> dtoClass = op.getResponseDtoClass();
            
            Type type;
            if (dtoClass == null) {
                type = new TypeToken<ServerResponse<DTO>>(){}.getType();
            } else {
                type = TypeToken.getParameterized(ServerResponse.class, dtoClass).getType();
            }
            
            ServerResponse<? extends DTO> response = gson.fromJson(jsonRes, type);
            
            // 4. Feedback generico (mostra sempre lo stato/messaggio del server)
            view.showResponse(response);
            
            // 5. Rendering automatico se ci sono dati e l'operazione è riuscita
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
}
