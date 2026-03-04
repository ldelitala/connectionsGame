package com.connections.client.modules;

import com.connections.common.protocol.requests.Request;
import com.connections.common.protocol.responses.Response;

/**
 * Interface for all specialized modules (User, Game, History).
 * Each module is responsible for parsing CLI commands into Requests and formatting Responses.
 */
public interface ClientModule {
    /**
     * Reads and parses a CLI command into a protocol Request.
     */
    Request parseCommand(String command, String[] tokens);

    /**
     * Formats and displays the response data in the best possible way for the CLI.
     */
    void displayResponse(Response<?> response);
}
