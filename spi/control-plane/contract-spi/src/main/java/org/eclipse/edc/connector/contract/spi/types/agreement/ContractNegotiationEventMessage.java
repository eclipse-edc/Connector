/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.spi.types.agreement;

import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;

import java.util.Objects;

public class ContractNegotiationEventMessage implements ContractRemoteMessage {

    private String protocol;
    private String callbackAddress;
    private String processId;
    private Type type;

    private String checksum;

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

    public Type getType() {
        return type;
    }

    public String getChecksum() {
        return checksum;
    }

    public static class Builder {
        private final ContractNegotiationEventMessage message;

        private Builder() {
            this.message = new ContractNegotiationEventMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            this.message.protocol = protocol;
            return this;
        }

        public Builder callbackAddress(String callbackAddress) {
            this.message.callbackAddress = callbackAddress;
            return this;
        }

        public Builder processId(String processId) {
            this.message.processId = processId;
            return this;
        }

        public Builder type(Type type) {
            this.message.type = type;
            return this;
        }

        public Builder checksum(String checksum) {
            this.message.checksum = checksum;
            return this;
        }

        public ContractNegotiationEventMessage build() {
            Objects.requireNonNull(message.protocol, "protocol");
            Objects.requireNonNull(message.processId, "processId");
            Objects.requireNonNull(message.type, "type");
            return message;
        }
    }

    public enum Type {
        ACCEPTED, FINALIZED
    }
}
