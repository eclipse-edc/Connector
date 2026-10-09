/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext.APPROVAL_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractNegotiationApprovalGuardTest {

    private final PolicyEngine policyEngine = mock();
    private final ContractNegotiationApprovalGuard guard = new ContractNegotiationApprovalGuard(policyEngine, mock());

    @Test
    void shouldHold_whenApprovalPolicyFails() {
        var policy = Policy.Builder.newInstance().build();
        when(policyEngine.evaluate(any(), any())).thenReturn(Result.failure("approval needed"));

        var result = guard.test(negotiation(PROVIDER, REQUESTED, policy));

        assertThat(result).isTrue();
        var captor = ArgumentCaptor.forClass(ApprovalContractNegotiationPolicyContext.class);
        verify(policyEngine).evaluate(eq(policy), captor.capture());
        var context = captor.getValue();
        assertThat(context.scope()).isEqualTo(APPROVAL_SCOPE);
        assertThat(context.participantContextId()).isEqualTo("participantContextId");
        assertThat(context.participantAgent().getIdentity()).isEqualTo("counter-party");
        assertThat(context.participantAgent().getClaims()).isEmpty();
    }

    @Test
    void shouldNotHold_whenApprovalPolicySucceeds() {
        when(policyEngine.evaluate(any(), any())).thenReturn(Result.success());

        var result = guard.test(negotiation(PROVIDER, REQUESTED, Policy.Builder.newInstance().build()));

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotHold_whenConsumer() {
        var result = guard.test(negotiation(CONSUMER, REQUESTED, Policy.Builder.newInstance().build()));

        assertThat(result).isFalse();
        verifyNoInteractions(policyEngine);
    }

    @ParameterizedTest
    @EnumSource(value = ContractNegotiationStates.class, mode = EXCLUDE, names = "REQUESTED")
    void shouldNotHold_whenNotRequested(ContractNegotiationStates state) {
        var result = guard.test(negotiation(PROVIDER, state, Policy.Builder.newInstance().build()));

        assertThat(result).isFalse();
        verifyNoInteractions(policyEngine);
    }

    @Test
    void shouldNotHold_whenNoOffer() {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id("id")
                .type(PROVIDER)
                .state(REQUESTED.code())
                .participantContextId("participantContextId")
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .protocol("protocol")
                .build();

        var result = guard.test(negotiation);

        assertThat(result).isFalse();
        verifyNoInteractions(policyEngine);
    }

    private ContractNegotiation negotiation(ContractNegotiation.Type type, ContractNegotiationStates state, Policy policy) {
        return ContractNegotiation.Builder.newInstance()
                .id("id")
                .type(type)
                .state(state.code())
                .participantContextId("participantContextId")
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .protocol("protocol")
                .contractOffer(ContractOffer.Builder.newInstance().id("offer-id").assetId("asset-id").policy(policy).build())
                .build();
    }
}
