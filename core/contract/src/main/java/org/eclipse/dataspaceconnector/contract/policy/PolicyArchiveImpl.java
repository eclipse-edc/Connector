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

package org.eclipse.dataspaceconnector.contract.policy;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyArchive;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

import java.util.Optional;

public class PolicyArchiveImpl implements PolicyArchive {
    private final ContractNegotiationStore contractNegotiationStore;
    private final PolicyStore policyStore;

    public PolicyArchiveImpl(ContractNegotiationStore contractNegotiationStore, PolicyStore policyStore) {
        this.contractNegotiationStore = contractNegotiationStore;
        this.policyStore = policyStore;
    }

    @Override
    public Policy findPolicyForContract(String contractId) {
        return Optional.ofNullable(contractId)
                .map(contractNegotiationStore::findContractAgreement)
                .map(ContractAgreement::getPolicyId)
                .map(policyStore::findById)
                .orElse(null);
    }

}
