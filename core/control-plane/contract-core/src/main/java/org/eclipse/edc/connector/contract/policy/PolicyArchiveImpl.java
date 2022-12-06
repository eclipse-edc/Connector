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

package org.eclipse.edc.connector.contract.policy;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.policy.model.Policy;

import java.util.Optional;

public class PolicyArchiveImpl implements PolicyArchive {
    private final ContractNegotiationStore contractNegotiationStore;

    public PolicyArchiveImpl(ContractNegotiationStore contractNegotiationStore) {
        this.contractNegotiationStore = contractNegotiationStore;
    }

    @Override
    public Policy findPolicyForContract(String contractId) {
        return Optional.ofNullable(contractId)
                .map(contractNegotiationStore::findContractAgreement)
                .map(ContractAgreement::getPolicy)
                .orElse(null);
    }

}
