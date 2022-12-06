/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.protocol.ids.spi.transform;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.model.Policy;

public class ContractAgreementTransformerOutput {
    private final ContractAgreement contractAgreement;
    private final Policy policy;

    private ContractAgreementTransformerOutput(ContractAgreement contractAgreement, Policy policy) {
        this.contractAgreement = contractAgreement;
        this.policy = policy;
    }

    public ContractAgreement getContractAgreement() {
        return contractAgreement;
    }

    public Policy getPolicy() {
        return policy;
    }

    public static class Builder {
        private ContractAgreement contractAgreement;
        private Policy policy;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder contractAgreement(ContractAgreement contractAgreement) {
            this.contractAgreement = contractAgreement;
            return this;
        }

        public Builder policy(Policy policy) {
            this.policy = policy;
            return this;
        }

        public ContractAgreementTransformerOutput build() {
            return new ContractAgreementTransformerOutput(contractAgreement, policy);
        }
    }
}
