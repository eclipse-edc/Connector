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

package org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * The {@link TransferStartMessage} is sent by the provider to indicate the asset transfer has been initiated.
 */
public class TransferStartMessage extends TransferRemoteMessage {

    private DataAddress dataAddress;

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TransferRemoteMessage.Builder<TransferStartMessage, Builder> {

        private Builder() {
            super(new TransferStartMessage());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dataAddress(DataAddress dataAddress) {
            message.dataAddress = dataAddress;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
