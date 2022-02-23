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

public class ContractRejectionMessage implements RemoteMessage {

    private String protocol;
    private String connectorId;
    private String connectorAddress;
    /**
     * Message ID of the ContractOfferRequest, that is rejected.
     */
    private String contractOfferMessageId;
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

    public String getContractOfferMessageId() {
        return contractOfferMessageId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public static class Builder {
        private final ContractRejectionMessage contractRejectionMessage;

        private Builder() {
            this.contractRejectionMessage = new ContractRejectionMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            this.contractRejectionMessage.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            this.contractRejectionMessage.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            this.contractRejectionMessage.connectorAddress = connectorAddress;
            return this;
        }

        public Builder correlationMessageId(String correlationMessageId) {
            this.contractRejectionMessage.contractOfferMessageId = correlationMessageId;
            return this;
        }

        public Builder rejectionReason(String rejectionReason) {
            this.contractRejectionMessage.rejectionReason = rejectionReason;
            return this;
        }

        public ContractRejectionMessage build() {
            Objects.requireNonNull(contractRejectionMessage.protocol, "protocol");
            Objects.requireNonNull(contractRejectionMessage.connectorId, "connectorId");
            Objects.requireNonNull(contractRejectionMessage.connectorAddress, "connectorAddress");
            Objects.requireNonNull(contractRejectionMessage.contractOfferMessageId, "correlationMessageId");
            Objects.requireNonNull(contractRejectionMessage.rejectionReason, "rejectionReason");
            return contractRejectionMessage;
        }
    }
}
