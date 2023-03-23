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

import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.Objects;

public class ContractNegotiationEventMessage implements RemoteMessage {

    private String protocol;
    private String connectorAddress;
    private String correlationId;
    private Type type;

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Type getType() {
        return type;
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

        public Builder connectorAddress(String connectorAddress) {
            this.message.connectorAddress = connectorAddress;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.message.correlationId = correlationId;
            return this;
        }

        public Builder type(Type type) {
            this.message.type = type;
            return this;
        }

        public ContractNegotiationEventMessage build() {
            Objects.requireNonNull(message.protocol, "protocol");
            Objects.requireNonNull(message.connectorAddress, "connectorAddress");
            Objects.requireNonNull(message.correlationId, "correlationId");
            Objects.requireNonNull(message.type, "type");
            return message;
        }
    }

    public enum Type {
        ACCEPTED, FINALIZED
    }
}
