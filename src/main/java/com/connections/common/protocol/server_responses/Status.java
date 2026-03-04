package com.connections.common.protocol.server_responses;

import com.google.gson.annotations.SerializedName;

/**
 * Enum representing the status of a server response.
 */
public enum Status {
    /** Indicates the operation was successful. */
    @SerializedName("ok")
    OK,

    /** Indicates the operation failed or an error occurred. */
    @SerializedName("error")
    ERROR,

    /** Indicates the operation is forbidden. */
    @SerializedName("forbidden")
    FORBIDDEN;
}
