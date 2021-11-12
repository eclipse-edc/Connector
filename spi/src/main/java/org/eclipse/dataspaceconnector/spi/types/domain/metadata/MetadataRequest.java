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

public class MetadataRequest implements RemoteMessage {

    private String protocol;
    private String connectorId;
    private String connectorAddress;
    private URI requestedAsset;

    private MetadataRequest() { }

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

    public URI getRequestedAsset() {
        return requestedAsset;
    }

    public static class Builder {
        private final MetadataRequest metadataRequest;

        private Builder() {
            this.metadataRequest = new MetadataRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            metadataRequest.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            metadataRequest.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            metadataRequest.connectorAddress = connectorAddress;
            return this;
        }

        public Builder elementId(URI elementId) {
            metadataRequest.requestedAsset = elementId;
            return this;
        }

        public MetadataRequest build() {
            Objects.requireNonNull(metadataRequest.protocol, "protocol");
            Objects.requireNonNull(metadataRequest.connectorId, "connectorId");
            Objects.requireNonNull(metadataRequest.connectorAddress, "connectorAddress");
            return metadataRequest;
        }
    }

}
