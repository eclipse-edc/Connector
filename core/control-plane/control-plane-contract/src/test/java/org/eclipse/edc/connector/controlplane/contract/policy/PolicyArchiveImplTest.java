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
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyArchiveImplTest {

    private final ContractNegotiationStore contractNegotiationStore = mock(ContractNegotiationStore.class);
    private final PolicyArchiveImpl policyArchive = new PolicyArchiveImpl(contractNegotiationStore);

    @Test
    void shouldGetPolicyFromAgreement() {
        var policy = Policy.Builder.newInstance().assigner("assigner").assignee("assignee").build();
        var contractAgreement = createContractAgreement(policy);
        when(contractNegotiationStore.findContractAgreement("contractId")).thenReturn(contractAgreement);

        var result = policyArchive.findPolicyForContract("contractId");

        assertThat(result).usingRecursiveComparison().ignoringFields().isEqualTo(policy);

        assertThat(result.getAssigner()).isNotEqualTo(contractAgreement.getProviderId());
        assertThat(result.getAssignee()).isNotEqualTo(contractAgreement.getConsumerId());
    }

    @Test
    void shouldGetPolicyFromAgreement_WithAssigneeAndAssignedInferred() {
        var policy = Policy.Builder.newInstance().build();
        var contractAgreement = createContractAgreement(policy);
        when(contractNegotiationStore.findContractAgreement("contractId")).thenReturn(contractAgreement);

        var result = policyArchive.findPolicyForContract("contractId");

        assertThat(result).usingRecursiveComparison().ignoringFields("assignee", "assigner").isEqualTo(policy);

        assertThat(result.getAssigner()).isEqualTo(contractAgreement.getProviderId());
        assertThat(result.getAssignee()).isEqualTo(contractAgreement.getConsumerId());
    }


    @Test
    void shouldReturnNullIfContractDoesNotExist() {
        when(contractNegotiationStore.findContractAgreement("contractId")).thenReturn(null);

        var result = policyArchive.findPolicyForContract("contractId");

        assertThat(result).isNull();
    }

    @Test
    void shouldGetAgreementIdFromAgreement() {
        var policy = Policy.Builder.newInstance().assigner("assigner").assignee("assignee").build();
        var contractAgreement = createContractAgreementBuilder(policy).agreementId("agreementId").build();
        when(contractNegotiationStore.findContractAgreement("contractId")).thenReturn(contractAgreement);

        var result = policyArchive.getAgreementIdForContract("contractId");

        assertThat(result).isEqualTo("agreementId");

    }

    @Test
    void shouldReturnAgreementIdNullIfContractDoesNotExist() {
        when(contractNegotiationStore.findContractAgreement("contractId")).thenReturn(null);

        var result = policyArchive.getAgreementIdForContract("contractId");

        assertThat(result).isNull();
    }

    private ContractAgreement createContractAgreement(Policy policyId) {
        return createContractAgreementBuilder(policyId)
                .build();
    }

    private ContractAgreement.Builder createContractAgreementBuilder(Policy policyId) {
        return ContractAgreement.Builder.newInstance()
                .id("any")
                .consumerId("any")
                .providerId("any")
                .assetId("any")
                .policy(policyId);
    }
}
