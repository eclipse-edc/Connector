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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.util.UUID.randomUUID;

/**
 * The {@link TransferStartMessage} is sent by the provider to indicate the asset transfer has been initiated.
 */
public class TransferStartMessage implements TransferRemoteMessage {

    private String id;
    private String counterPartyAddress;
    private String protocol = "unknown";
    private String processId;

    private DataAddress dataAddress;

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String protocol) {
        Objects.requireNonNull(protocol);
        this.protocol = protocol;
    }

    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    @Override
    @NotNull
    public String getProcessId() {
        return processId;
    }

    public DataAddress getDataAddress() {
        return dataAddress;
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

        public Builder id(String id) {
            this.message.id = id;
            return this;
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            message.counterPartyAddress = counterPartyAddress;
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

        public Builder dataAddress(DataAddress dataAddress) {
            message.dataAddress = dataAddress;
            return this;
        }

        public TransferStartMessage build() {
            if (message.id == null) {
                message.id = randomUUID().toString();
            }

            Objects.requireNonNull(message.processId, "The processId must be specified");
            return message;
        }

    }
}
