package com.connections.common;

import com.google.gson.annotations.SerializedName;

/**
 * Base abstract class for all client requests.
 */
public abstract class Request {
    @SerializedName("operation")
    private final String operationValue;

    public Request(Operation operation) {
        this.operationValue = (operation != null) ? operation.getValue() : null;
    }

    public String getOperation() {
        return operationValue;
    }
}
