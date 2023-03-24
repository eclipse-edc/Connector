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
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.Objects;

/**
 * The {@link TransferStartMessage} is sent by the provider to indicate the asset transfer has been initiated.
 */
public class TransferStartMessage implements RemoteMessage {

    private String connectorAddress;
    private String protocol;

    private String correlationId;

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getCorrelationId(){return correlationId;}

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

        public Builder correlationId(String correlationId){
            message.correlationId = correlationId;
            return this;
        }

        public TransferStartMessage build() {
            Objects.requireNonNull(message.protocol, "The protocol must be specified");
            Objects.requireNonNull(message.connectorAddress, "The connectorAddress must be specified");
            Objects.requireNonNull(message.correlationId, "The correlationID must be specified");
            return message;
        }

    }
}
