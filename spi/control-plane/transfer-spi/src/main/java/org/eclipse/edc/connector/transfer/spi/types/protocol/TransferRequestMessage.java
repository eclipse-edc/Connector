/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.spi.types.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The {@link TransferRequestMessage} is sent by a consumer to initiate a transfer process.
 */
public class TransferRequestMessage implements TransferRemoteMessage {

    private String connectorAddress;
    private String protocol;
    private String id;
    private String contractId;
    private String assetId; // TODO remove when removing ids module
    private DataAddress dataDestination;
    private String connectorId; // TODO remove when removing ids module
    private Map<String, String> properties = new HashMap<>();

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getConnectorAddress() {
        return connectorAddress;
    }

    @Override
    public String getProcessId() {
        return id;
    }

    @Deprecated
    public String getAssetId() {
        return assetId;
    }

    public String getContractId() {
        return contractId;
    }

    public String getId() {
        return id;
    }

    @Deprecated
    public String getConnectorId() {
        return connectorId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public DataAddress getDataDestination() {
        return dataDestination;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TransferRequestMessage message;

        private Builder() {
            message = new TransferRequestMessage();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            message.id = id;
            return this;
        }

        public Builder connectorAddress(String address) {
            message.connectorAddress = address;
            return this;
        }

        public Builder protocol(String protocol) {
            message.protocol = protocol;
            return this;
        }

        public Builder contractId(String contractId) {
            message.contractId = contractId;
            return this;
        }

        @Deprecated
        public Builder assetId(String assetId) {
            message.assetId = assetId;
            return this;
        }

        public Builder dataDestination(DataAddress dataDestination) {
            message.dataDestination = dataDestination;
            return this;
        }

        @Deprecated
        public Builder connectorId(String connectorId) {
            message.connectorId = connectorId;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            message.properties = properties;
            return this;
        }

        public TransferRequestMessage build() {
            Objects.requireNonNull(message.protocol, "The protocol must be specified");
            Objects.requireNonNull(message.connectorAddress, "The connectorAddress must be specified");
            return message;
        }
    }
}
