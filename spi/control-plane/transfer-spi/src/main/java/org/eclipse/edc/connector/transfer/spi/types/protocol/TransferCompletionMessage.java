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

public class TransferCompletionMessage implements RemoteMessage {

    private String connectorAddress;
    private String protocol;

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getConnectorAddress() {
        return connectorAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TransferCompletionMessage message;

        private Builder() {
            message = new TransferCompletionMessage();
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

        public TransferCompletionMessage build() {
            Objects.requireNonNull(message.protocol, "The protocol must be specified");
            Objects.requireNonNull(message.connectorAddress, "The connectorAddress must be specified");
            return message;
        }

    }
}
