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

package org.eclipse.edc.connector.controlplane.contract.policy;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
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
                .map(this::mapAgreementPolicy)
                .orElse(null);
    }

    @Override
    public String getAgreementIdForContract(String contractId) {
        return Optional.ofNullable(contractId)
                .map(contractNegotiationStore::findContractAgreement)
                .map(ContractAgreement::getAgreementId)
                .orElse(null);
    }

    // TODO assignee and assigner should end up stored in the Agreement's policy as outlined here
    //  https://github.com/International-Data-Spaces-Association/ids-specification/issues/195
    //  As fallback we fill the assignee and the assigner from the consumer and provider id in
    //  the contract agreement

    private Policy mapAgreementPolicy(ContractAgreement contractAgreement) {
        var policy = contractAgreement.getPolicy();
        var assignee = Optional.ofNullable(policy.getAssignee())
                .orElseGet(contractAgreement::getConsumerId);

        var assigner = Optional.ofNullable(policy.getAssigner())
                .orElseGet(contractAgreement::getProviderId);

        return policy.toBuilder()
                .assignee(assignee)
                .assigner(assigner)
                .build();
    }

}
