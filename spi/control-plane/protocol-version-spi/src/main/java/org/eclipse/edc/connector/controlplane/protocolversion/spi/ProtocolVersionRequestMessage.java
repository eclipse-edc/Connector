/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.protocolversion.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.Objects;

public class ProtocolVersionRequestMessage implements RemoteMessage {

    private final Policy policy;

    private String protocol = "unknown";
    private String counterPartyAddress;
    private String counterPartyId;

    private ProtocolVersionRequestMessage() {
        // at this time, this is just a placeholder.
        policy = Policy.Builder.newInstance().build();
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    @Override
    public String getCounterPartyId() {
        return counterPartyId;
    }

    public Policy getPolicy() {
        return policy;
    }

    public static class Builder {
        private final ProtocolVersionRequestMessage message;

        private Builder() {
            message = new ProtocolVersionRequestMessage();
        }

        @JsonCreator
        public static ProtocolVersionRequestMessage.Builder newInstance() {
            return new ProtocolVersionRequestMessage.Builder();
        }

        public ProtocolVersionRequestMessage.Builder protocol(String protocol) {
            this.message.protocol = protocol;
            return this;
        }

        public ProtocolVersionRequestMessage.Builder counterPartyAddress(String callbackAddress) {
            this.message.counterPartyAddress = callbackAddress;
            return this;
        }

        public ProtocolVersionRequestMessage.Builder counterPartyId(String counterPartyId) {
            this.message.counterPartyId = counterPartyId;
            return this;
        }

        public ProtocolVersionRequestMessage build() {
            Objects.requireNonNull(message.protocol, "protocol");

            return message;
        }

    }
}
