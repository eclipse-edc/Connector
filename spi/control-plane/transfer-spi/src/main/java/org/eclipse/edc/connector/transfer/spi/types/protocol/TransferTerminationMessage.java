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
 * The {@link TransferTerminationMessage} is sent by the provider or consumer at any point except a terminal state to
 * indicate the data transfer process should stop and be placed in a terminal state. If the termination was due to an
 * error, the sender may include error information.
 */
public class TransferTerminationMessage implements TransferRemoteMessage {

    private String callbackAddress;
    private String protocol;
    private String processId;

    private String code;

    private String reason; //TODO change to List  https://github.com/eclipse-edc/Connector/issues/2729

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getCallbackAddress() {
        return callbackAddress;
    }

    @Override
    public String getProcessId() {
        return processId;
    }

    public String getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TransferTerminationMessage message;

        private Builder() {
            message = new TransferTerminationMessage();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder callbackAddress(String address) {
            message.callbackAddress = address;
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

        public Builder code(String code) {
            message.code = code;
            return this;
        }

        public Builder reason(String reason) {
            message.reason = reason;
            return this;
        }

        public TransferTerminationMessage build() {
            Objects.requireNonNull(message.protocol, "The protocol must be specified");
            Objects.requireNonNull(message.processId, "The processId must be specified");
            //TODO add Nullcheck for message.code Issue https://github.com/eclipse-edc/Connector/issues/2810
            return message;
        }
    }
}
