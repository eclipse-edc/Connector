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

package org.eclipse.edc.spi.types.domain.edr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Sends an endpoint reference to an external system.
 *
 * @deprecated only used by ids multipart api/dispatcher. Can be removed with IDS modules.
 */
@Deprecated(forRemoval = true)
@JsonDeserialize(builder = EndpointDataReferenceMessage.Builder.class)
public class EndpointDataReferenceMessage implements RemoteMessage {

    private final String connectorId;
    private final String callbackAddress;
    private final String protocol;
    private final EndpointDataReference endpointDataReference;

    private EndpointDataReferenceMessage(String connectorId, String callbackAddress, String protocol, EndpointDataReference endpointDataReference) {
        this.connectorId = connectorId;
        this.callbackAddress = callbackAddress;
        this.protocol = protocol;
        this.endpointDataReference = endpointDataReference;
    }

    @NotNull
    public String getConnectorId() {
        return connectorId;
    }

    @NotNull
    @Override
    public String getCounterPartyAddress() {
        return callbackAddress;
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
        private String callbackAddress;
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

        public EndpointDataReferenceMessage.Builder callbackAddress(String callbackAddress) {
            this.callbackAddress = callbackAddress;
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
            Objects.requireNonNull(callbackAddress, "callbackAddress");
            Objects.requireNonNull(endpointDataReference, "endpointDataReference");

            return new EndpointDataReferenceMessage(connectorId, callbackAddress, protocol, endpointDataReference);
        }
    }
}
