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
 * The {@link TransferTerminationMessage} is sent by the provider or consumer at any point except a terminal state to
 * indicate the data transfer process should stop and be placed in a terminal state. If the termination was due to an
 * error, the sender may include error information.
 */
public class TransferTerminationMessage extends TransferRemoteMessage {

    private String code;
    private String reason; //TODO change to List  https://github.com/eclipse-edc/Connector/issues/2729

    public String getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TransferRemoteMessage.Builder<TransferTerminationMessage, Builder> {

        private Builder() {
            super(new TransferTerminationMessage());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder code(String code) {
            message.code = code;
            return self();
        }

        public Builder reason(String reason) {
            message.reason = reason;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
