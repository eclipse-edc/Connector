/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.nifi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("dagx:nifipayload")
public final class NifiPayload {

    private NifiTransferEndpoint source;
    private NifiTransferEndpoint destination;
    private String requestId;

    @JsonCreator
    public NifiPayload(@JsonProperty("requestId") String requestId, @JsonProperty("source") NifiTransferEndpoint source, @JsonProperty("destination") NifiTransferEndpoint destination) {
        this.requestId = requestId;
        this.source = source;
        this.destination = destination;
    }

    private NifiPayload() {
    }

    public NifiTransferEndpoint getSource() {
        return source;
    }

    public NifiTransferEndpoint getDestination() {
        return destination;
    }

    public String getRequestId() {
        return requestId;
    }
}
