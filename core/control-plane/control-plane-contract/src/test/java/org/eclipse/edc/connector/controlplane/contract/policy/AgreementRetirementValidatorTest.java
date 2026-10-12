/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.policy;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgreementRetirementValidatorTest {

    private final AgreementRetirementValidator<AgreementPolicyContext> validator = new AgreementRetirementValidator<>();
    private final AgreementPolicyContext context = mock();
    private final Policy policy = Policy.Builder.newInstance().build();

    @Test
    void shouldFail_whenAgreementIsRetired() {
        when(context.contractAgreement()).thenReturn(agreement(true));

        var result = validator.apply(policy, context);

        assertThat(result).isFalse();
        verify(context).reportProblem(anyString());
    }

    @Test
    void shouldPass_whenAgreementIsNotRetired() {
        when(context.contractAgreement()).thenReturn(agreement(false));

        var result = validator.apply(policy, context);

        assertThat(result).isTrue();
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void shouldPass_whenAgreementIsNull() {
        when(context.contractAgreement()).thenReturn(null);

        var result = validator.apply(policy, context);

        assertThat(result).isTrue();
        verify(context, never()).reportProblem(anyString());
    }

    private ContractAgreement agreement(boolean retired) {
        return ContractAgreement.Builder.newInstance()
                .id("agreement-id")
                .providerId("provider")
                .consumerId("consumer")
                .assetId("asset")
                .policy(policy)
                .retired(retired)
                .build();
    }
}
