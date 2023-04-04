/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import java.util.Objects;

/**
 * The {@link TransferStartMessage} is sent by the provider to indicate the asset transfer has been initiated.
 */
public class TransferStartMessage implements TransferRemoteMessage {

    private String connectorAddress;
    private String protocol;
    private String processId;

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
        return processId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TransferStartMessage message;

        private Builder() {
            message = new TransferStartMessage();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder connectorAddress(String address) {
            message.connectorAddress = address;
            return this;
        }

        public Builder protocol(String protocol) {
            message.protocol = protocol;
            return this;
        }

        public Builder processId(String processId) {
            message.processId = processId;
            return this;
        }

        public TransferStartMessage build() {
            Objects.requireNonNull(message.protocol, "The protocol must be specified");
            Objects.requireNonNull(message.connectorAddress, "The connectorAddress must be specified");
            Objects.requireNonNull(message.processId, "The processId must be specified");
            return message;
        }

    }
}
