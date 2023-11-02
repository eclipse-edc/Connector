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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.validation;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.CONTRACT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractValidationRuleTest {

    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);
    private final ContractNegotiationStore contractNegotiationStore = mock(ContractNegotiationStore.class);

    private ContractValidationRule rule;

    @BeforeEach
    public void setUp() {
        rule = new ContractValidationRule(contractNegotiationStore, clock);
    }

    @Test
    void shouldSucceedIfContractIsStillValid() {
        var contractId = UUID.randomUUID().toString();
        var contractAgreement = createContractAgreement(contractId);
        when(contractNegotiationStore.findContractAgreement(contractId)).thenReturn(contractAgreement);
        var claimToken = ClaimToken.Builder.newInstance().claim(CONTRACT_ID, contractId).build();

        var result = rule.checkRule(claimToken, emptyMap());

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldFailIfContractIdClaimIsMissing() {
        var claimToken = ClaimToken.Builder.newInstance().build();

        var result = rule.checkRule(claimToken, emptyMap());

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void shouldFailIfContractIdContractDoesNotExist() {
        when(contractNegotiationStore.findContractAgreement(any())).thenReturn(null);
        var claimToken = ClaimToken.Builder.newInstance().claim(CONTRACT_ID, "unknownContractId").build();

        var result = rule.checkRule(claimToken, emptyMap());

        assertThat(result.succeeded()).isFalse();
    }

    private ContractAgreement createContractAgreement(String contractId) {
        return ContractAgreement.Builder.newInstance()
                .id(contractId)
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .consumerId("consumer-agent-id")
                .providerId("provider-agent-id")
                .build();
    }
}
