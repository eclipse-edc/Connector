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
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

import java.net.URI;
import java.util.Objects;

public class DescriptionRequest implements RemoteMessage {

    private String protocol;
    private String connectorId;
    private String connectorAddress;
    private URI requestedElement;

    private DescriptionRequest() { }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getConnectorAddress() {
        return connectorAddress;
    }

    public URI getRequestedElement() {
        return requestedElement;
    }

    public static class Builder {
        private final DescriptionRequest descriptionRequest;

        private Builder() {
            this.descriptionRequest = new DescriptionRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            descriptionRequest.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            descriptionRequest.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            descriptionRequest.connectorAddress = connectorAddress;
            return this;
        }

        public Builder elementId(URI elementId) {
            descriptionRequest.requestedElement = elementId;
            return this;
        }

        public DescriptionRequest build() {
            Objects.requireNonNull(descriptionRequest.protocol, "protocol");
            Objects.requireNonNull(descriptionRequest.connectorId, "connectorId");
            Objects.requireNonNull(descriptionRequest.connectorAddress, "connectorAddress");
            return descriptionRequest;
        }
    }

}
