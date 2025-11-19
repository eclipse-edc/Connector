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

/**
 * The {@link TransferCompletionMessage} is sent by the provider or consumer when asset transfer has completed. Note
 * that some data plane implementations may optimize completion notification by performing it as part of its wire
 * protocol. In those cases, a {@link TransferCompletionMessage} message does not need to be sent.
 */
public class TransferCompletionMessage extends TransferRemoteMessage {

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TransferRemoteMessage.Builder<TransferCompletionMessage, Builder> {

        private Builder() {
            super(new TransferCompletionMessage());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
