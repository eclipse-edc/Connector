/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */


package org.eclipse.dataspaceconnector.spi.types.domain.edr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@JsonDeserialize(builder = EndpointDataReferenceRequest.Builder.class)
public class EndpointDataReferenceRequest implements RemoteMessage {

    private final String connectorId;
    private final String connectorAddress;
    private final String protocol;
    private final EndpointDataReference endpointDataReference;

    private EndpointDataReferenceRequest(@NotNull String connectorId, @NotNull String connectorAddress, @NotNull String protocol, @NotNull EndpointDataReference endpointDataReference) {
        this.connectorId = connectorId;
        this.connectorAddress = connectorAddress;
        this.protocol = protocol;
        this.endpointDataReference = endpointDataReference;
    }

    @NotNull
    public String getConnectorId() {
        return connectorId;
    }

    @NotNull
    public String getConnectorAddress() {
        return connectorAddress;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @NotNull
    public EndpointDataReference getEndpointDataReference() {
        return endpointDataReference;
    }

    public static class Builder {
        private String connectorId;
        private String connectorAddress;
        private String protocol;
        private EndpointDataReference endpointDataReference;

        private Builder() {
        }

        @JsonCreator
        public static EndpointDataReferenceRequest.Builder newInstance() {
            return new EndpointDataReferenceRequest.Builder();
        }

        public EndpointDataReferenceRequest.Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public EndpointDataReferenceRequest.Builder connectorAddress(String connectorAddress) {
            this.connectorAddress = connectorAddress;
            return this;
        }

        public EndpointDataReferenceRequest.Builder connectorId(String connectorId) {
            this.connectorId = connectorId;
            return this;
        }

        public EndpointDataReferenceRequest.Builder endpointDataReference(EndpointDataReference endpointDataReference) {
            this.endpointDataReference = endpointDataReference;
            return this;
        }

        public EndpointDataReferenceRequest build() {
            Objects.requireNonNull(protocol, "protocol");
            Objects.requireNonNull(connectorId, "connectorId");
            Objects.requireNonNull(connectorAddress, "connectorAddress");
            Objects.requireNonNull(endpointDataReference, "endpointDataReference");

            return new EndpointDataReferenceRequest(connectorId, connectorAddress, protocol, endpointDataReference);
        }
    }
}
