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

package org.eclipse.edc.connector.contract.spi.types.negotiation;

import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;

import java.util.Objects;

public class ContractNegotiationTerminationMessage implements ContractRemoteMessage {

    private String protocol;
    private String connectorId; // TODO remove when removing ids module
    private String connectorAddress;
    private String correlationId;
    private String rejectionReason; // TODO change to list https://github.com/eclipse-edc/Connector/issues/2729

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getConnectorAddress() {
        return connectorAddress;
    }

    @Deprecated
    public String getConnectorId() {
        return connectorId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    @Override
    public String getProcessId() {
        return getCorrelationId();
    }

    public static class Builder {
        private final ContractNegotiationTerminationMessage contractNegotiationTerminationMessage;

        private Builder() {
            this.contractNegotiationTerminationMessage = new ContractNegotiationTerminationMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            this.contractNegotiationTerminationMessage.protocol = protocol;
            return this;
        }

        @Deprecated
        public Builder connectorId(String connectorId) {
            this.contractNegotiationTerminationMessage.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            this.contractNegotiationTerminationMessage.connectorAddress = connectorAddress;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.contractNegotiationTerminationMessage.correlationId = correlationId;
            return this;
        }

        public Builder rejectionReason(String rejectionReason) {
            this.contractNegotiationTerminationMessage.rejectionReason = rejectionReason;
            return this;
        }

        public ContractNegotiationTerminationMessage build() {
            Objects.requireNonNull(contractNegotiationTerminationMessage.protocol, "protocol");
            Objects.requireNonNull(contractNegotiationTerminationMessage.connectorAddress, "connectorAddress");
            Objects.requireNonNull(contractNegotiationTerminationMessage.correlationId, "correlationId");
            Objects.requireNonNull(contractNegotiationTerminationMessage.rejectionReason, "rejectionReason");
            return contractNegotiationTerminationMessage;
        }
    }
}
