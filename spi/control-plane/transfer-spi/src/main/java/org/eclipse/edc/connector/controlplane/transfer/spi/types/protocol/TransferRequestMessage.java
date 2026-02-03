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

package org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Objects;

/**
 * The {@link TransferRequestMessage} is sent by a consumer to initiate a transfer process.
 */
public class TransferRequestMessage extends TransferRemoteMessage {

    private String contractId;
    private DataAddress dataAddress;
    private String transferType;
    private String callbackAddress;

    public String getContractId() {
        return contractId;
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public String getTransferType() {
        return transferType;
    }

    public String getCallbackAddress() {
        return callbackAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TransferRemoteMessage.Builder<TransferRequestMessage, Builder> {

        private Builder() {
            super(new TransferRequestMessage());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder callbackAddress(String callbackAddress) {
            message.callbackAddress = callbackAddress;
            return this;
        }

        public Builder contractId(String contractId) {
            message.contractId = contractId;
            return this;
        }

        public Builder dataAddress(DataAddress dataAddress) {
            message.dataAddress = dataAddress;
            return this;
        }

        public Builder transferType(String transferType) {
            message.transferType = transferType;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public TransferRequestMessage build() {
            Objects.requireNonNull(message.callbackAddress, "The callbackAddress must be specified");
            return super.build();
        }
    }
}
