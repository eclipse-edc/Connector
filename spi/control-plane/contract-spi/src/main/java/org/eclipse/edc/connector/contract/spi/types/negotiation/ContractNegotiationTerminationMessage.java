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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ContractNegotiationTerminationMessage implements ContractRemoteMessage {

    private String protocol;
    @Deprecated(forRemoval = true)
    private String connectorId;
    private String counterPartyAddress;
    private String processId;
    private String rejectionReason; // TODO change to list https://github.com/eclipse-edc/Connector/issues/2729
    private String code;

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    @Deprecated
    public String getConnectorId() {
        return connectorId;
    }

    @Override
    public String getProcessId() {
        return processId;
    }

    @Nullable
    public String getRejectionReason() {
        return rejectionReason;
    }

    @Nullable
    public String getCode() {
        return code;
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

        public Builder counterPartyAddress(String counterPartyAddress) {
            this.contractNegotiationTerminationMessage.counterPartyAddress = counterPartyAddress;
            return this;
        }

        public Builder processId(String processId) {
            this.contractNegotiationTerminationMessage.processId = processId;
            return this;
        }

        public Builder rejectionReason(String rejectionReason) {
            this.contractNegotiationTerminationMessage.rejectionReason = rejectionReason;
            return this;
        }

        public Builder code(String code) {
            this.contractNegotiationTerminationMessage.code = code;
            return this;
        }

        public ContractNegotiationTerminationMessage build() {
            Objects.requireNonNull(contractNegotiationTerminationMessage.protocol, "protocol");
            Objects.requireNonNull(contractNegotiationTerminationMessage.processId, "processId");
            return contractNegotiationTerminationMessage;
        }
    }
}
