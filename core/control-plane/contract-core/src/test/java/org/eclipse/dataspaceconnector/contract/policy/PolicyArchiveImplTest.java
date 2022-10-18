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
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyArchiveImplTest {

    private final ContractNegotiationStore contractNegotiationStore = mock(ContractNegotiationStore.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final PolicyArchiveImpl policyArchive = new PolicyArchiveImpl(contractNegotiationStore);

    @Test
    void shouldGetPolicyFromAgreement() {
        var policy = Policy.Builder.newInstance().build();
        var contractAgreement = createContractAgreement(policy);
        when(contractNegotiationStore.findContractAgreement("contractId")).thenReturn(contractAgreement);

        var result = policyArchive.findPolicyForContract("contractId");

        assertThat(result).usingRecursiveComparison().isEqualTo(policy);
    }

    @Test
    void shouldReturnNullIfContractDoesNotExist() {
        when(contractNegotiationStore.findContractAgreement("contractId")).thenReturn(null);

        var result = policyArchive.findPolicyForContract("contractId");

        assertThat(result).isNull();
    }

    private ContractAgreement createContractAgreement(Policy policyId) {
        return ContractAgreement.Builder.newInstance()
                .id("any")
                .consumerAgentId("any")
                .providerAgentId("any")
                .assetId("any")
                .policy(policyId)
                .build();
    }
}
