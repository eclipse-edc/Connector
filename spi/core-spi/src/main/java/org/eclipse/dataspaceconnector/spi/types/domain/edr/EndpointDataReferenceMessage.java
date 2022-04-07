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

/**
 * Sends an endpoint reference to an external system.
 */
@JsonDeserialize(builder = EndpointDataReferenceMessage.Builder.class)
public class EndpointDataReferenceMessage implements RemoteMessage {

    private final String connectorId;
    private final String connectorAddress;
    private final String protocol;
    private final EndpointDataReference endpointDataReference;

    private EndpointDataReferenceMessage(String connectorId, String connectorAddress, String protocol, EndpointDataReference endpointDataReference) {
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
        public static EndpointDataReferenceMessage.Builder newInstance() {
            return new EndpointDataReferenceMessage.Builder();
        }

        public EndpointDataReferenceMessage.Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public EndpointDataReferenceMessage.Builder connectorAddress(String connectorAddress) {
            this.connectorAddress = connectorAddress;
            return this;
        }

        public EndpointDataReferenceMessage.Builder connectorId(String connectorId) {
            this.connectorId = connectorId;
            return this;
        }

        public EndpointDataReferenceMessage.Builder endpointDataReference(EndpointDataReference endpointDataReference) {
            this.endpointDataReference = endpointDataReference;
            return this;
        }

        public EndpointDataReferenceMessage build() {
            Objects.requireNonNull(protocol, "protocol");
            Objects.requireNonNull(connectorId, "connectorId");
            Objects.requireNonNull(connectorAddress, "connectorAddress");
            Objects.requireNonNull(endpointDataReference, "endpointDataReference");

            return new EndpointDataReferenceMessage(connectorId, connectorAddress, protocol, endpointDataReference);
        }
    }
}
