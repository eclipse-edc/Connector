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
import org.eclipse.edc.policy.model.Policy;

import java.util.Objects;

public class ContractAgreementMessage implements ContractRemoteMessage {
    private String protocol;
    private String connectorId; // TODO remove when removing ids module
    private String connectorAddress;
    private String correlationId;
    private ContractAgreement contractAgreement;
    private Policy policy; // TODO remove when removing ids module

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

    public ContractAgreement getContractAgreement() {
        return contractAgreement;
    }

    @Deprecated
    public Policy getPolicy() {
        return policy;
    }

    @Override
    public String getProcessId() {
        return getCorrelationId();
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

        @Deprecated
        public Builder connectorId(String connectorId) {
            this.contractAgreementMessage.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            this.contractAgreementMessage.connectorAddress = connectorAddress;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.contractAgreementMessage.correlationId = correlationId;
            return this;
        }

        public Builder contractAgreement(ContractAgreement contractAgreement) {
            this.contractAgreementMessage.contractAgreement = contractAgreement;
            return this;
        }

        @Deprecated
        public Builder policy(Policy policy) {
            this.contractAgreementMessage.policy = policy;
            return this;
        }

        public ContractAgreementMessage build() {
            Objects.requireNonNull(contractAgreementMessage.protocol, "protocol");
            Objects.requireNonNull(contractAgreementMessage.connectorId, "connectorId");
            Objects.requireNonNull(contractAgreementMessage.connectorAddress, "connectorAddress");
            Objects.requireNonNull(contractAgreementMessage.contractAgreement, "contractAgreement");
            Objects.requireNonNull(contractAgreementMessage.policy, "policy");
            Objects.requireNonNull(contractAgreementMessage.correlationId, "correlationId");
            return contractAgreementMessage;
        }
    }
}
