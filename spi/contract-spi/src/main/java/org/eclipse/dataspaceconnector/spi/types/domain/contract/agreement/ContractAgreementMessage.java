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

package org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement;

import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

import java.util.Objects;

public class ContractAgreementMessage implements RemoteMessage {

    private String protocol;
    private String connectorId;
    private String connectorAddress;
    private String contractOfferMessageId;
    private ContractAgreement contractAgreement;

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

    public ContractAgreement getContractAgreement() {
        return contractAgreement;
    }

    public static class Builder {
        private final ContractAgreementMessage contractAgreementMessage;

        private Builder() {
            this.contractAgreementMessage = new ContractAgreementMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            this.contractAgreementMessage.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            this.contractAgreementMessage.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            this.contractAgreementMessage.connectorAddress = connectorAddress;
            return this;
        }

        public Builder contractOfferMessageId(String contractOfferMessageId) {
            this.contractAgreementMessage.contractOfferMessageId = contractOfferMessageId;
            return this;
        }

        public Builder contractAgreement(ContractAgreement contractAgreement) {
            this.contractAgreementMessage.contractAgreement = contractAgreement;
            return this;
        }

        public ContractAgreementMessage build() {
            Objects.requireNonNull(contractAgreementMessage.protocol, "protocol");
            Objects.requireNonNull(contractAgreementMessage.connectorId, "connectorId");
            Objects.requireNonNull(contractAgreementMessage.connectorAddress, "connectorAddress");
            Objects.requireNonNull(contractAgreementMessage.contractAgreement, "contractAgreement");
            Objects.requireNonNull(contractAgreementMessage.contractOfferMessageId, "contractOfferMessageId");
            return contractAgreementMessage;
        }
    }
}
