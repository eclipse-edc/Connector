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
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.util.UUID.randomUUID;

public class ContractAgreementMessage implements ContractRemoteMessage {

    private String id;
    private String protocol = "unknown";
    private String counterPartyAddress;
    private String processId;
    private ContractAgreement contractAgreement;

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String protocol) {
        Objects.requireNonNull(protocol);
        this.protocol = protocol;
    }

    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    @Override
    public @NotNull String getProcessId() {
        return processId;
    }

    public ContractAgreement getContractAgreement() {
        return contractAgreement;
    }

    public Policy getPolicy() {
        return contractAgreement.getPolicy();
    }

    public static class Builder {
        private final ContractAgreementMessage contractAgreementMessage;

        private Builder() {
            this.contractAgreementMessage = new ContractAgreementMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.contractAgreementMessage.id = id;
            return this;
        }

        public Builder protocol(String protocol) {
            this.contractAgreementMessage.protocol = protocol;
            return this;
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            this.contractAgreementMessage.counterPartyAddress = counterPartyAddress;
            return this;
        }

        public Builder processId(String processId) {
            this.contractAgreementMessage.processId = processId;
            return this;
        }

        public Builder contractAgreement(ContractAgreement contractAgreement) {
            this.contractAgreementMessage.contractAgreement = contractAgreement;
            return this;
        }

        public ContractAgreementMessage build() {
            if (contractAgreementMessage.id == null) {
                contractAgreementMessage.id = randomUUID().toString();
            }
            Objects.requireNonNull(contractAgreementMessage.contractAgreement, "contractAgreement");
            Objects.requireNonNull(contractAgreementMessage.processId, "processId");
            return contractAgreementMessage;
        }
    }
}
