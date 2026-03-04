package com.connections.client.modules;

import com.connections.common.protocol.requests.Request;
import com.connections.common.protocol.responses.Response;

/**
 * Interface for all client modules.
 * A module is responsible for parsing CLI commands and displaying their responses.
 */
public interface ClientModule {
    /**
     * Parses a CLI command into a protocol Request.
     * 
     * @param command the raw command string
     * @param tokens the command split into tokens
     * @return a Request object if the command is valid, null otherwise
     */
    Request parseCommand(String command, String[] tokens);

    /**
     * Formats and displays the response data to the user.
     * 
     * @param response the response received from the server
     */
    void displayResponse(Response<?> response);
}
