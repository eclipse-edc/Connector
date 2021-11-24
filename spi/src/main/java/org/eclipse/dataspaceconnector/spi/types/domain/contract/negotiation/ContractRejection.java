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

package org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation;

import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

import java.util.Objects;

public class ContractRejection implements RemoteMessage {

    private String protocol;
    private String connectorId;
    private String connectorAddress;
    private String correlatedContractId; // TODO hand over the contract offer/agreement - not an id?
    private String rejectionReason; // TODO pre-define a set of enums (+ mapping to IDS) ?

    @Override
    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getCorrelatedContractId() {
        return correlatedContractId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public static class Builder {
        private final ContractRejection contractRejection;

        private Builder() {
            this.contractRejection = new ContractRejection();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            this.contractRejection.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            this.contractRejection.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            this.contractRejection.connectorAddress = connectorAddress;
            return this;
        }

        public Builder correlatedContractId(String correlatedContractId) {
            this.contractRejection.correlatedContractId = correlatedContractId;
            return this;
        }

        public Builder rejectionReason(String rejectionReason) {
            this.contractRejection.rejectionReason = rejectionReason;
            return this;
        }

        public ContractRejection build() {
            Objects.requireNonNull(contractRejection.protocol, "protocol");
            Objects.requireNonNull(contractRejection.connectorId, "connectorId");
            Objects.requireNonNull(contractRejection.connectorAddress, "connectorAddress");
            Objects.requireNonNull(contractRejection.correlatedContractId, "correlatedContractId");
            Objects.requireNonNull(contractRejection.rejectionReason, "rejectionReason");
            return contractRejection;
        }
    }
}
