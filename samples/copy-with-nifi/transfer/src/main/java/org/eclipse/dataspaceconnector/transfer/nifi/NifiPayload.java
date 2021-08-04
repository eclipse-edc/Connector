/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.nifi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("dataspaceconnector:nifipayload")
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
