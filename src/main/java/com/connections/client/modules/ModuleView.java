package com.connections.client.modules;

import com.connections.common.protocol.server_responses.ServerResponse;

/**
 * Interface for modules that can display a server response.
 */
public interface ModuleView {
    void displayResponse(ServerResponse<?> response);
}
