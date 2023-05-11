/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.contract.validation;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractExpiryCheckFunctionTest {

    private final ContractExpiryCheckFunction function = new ContractExpiryCheckFunction();
    private final PolicyContext contextMock = mock(PolicyContext.class);

    @Test
    void evaluate_noAgreementFound() {
        var result = function.evaluate(Operator.LEQ, "bar", Permission.Builder.newInstance().build(), contextMock);
        assertThat(result).isFalse();
        verify(contextMock).reportProblem(contains("ContractAgreement"));
    }

    @Test
    void evaluate_fixed_contractNotYetStarted() {

        var fixedInForceTimeConstraint = AndConstraint.Builder.newInstance()
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression(EDC_NAMESPACE + "inForceDate"))
                        .operator(Operator.GEQ)
                        .rightExpression(new LiteralExpression(Instant.now().toString()))
                        .build())
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression(EDC_NAMESPACE + "inForceDate"))
                        .operator(Operator.LEQ)
                        .rightExpression(new LiteralExpression(Instant.now().plus(ofHours(1)).toString()))
                        .build())
                .build();
        var permission = Permission.Builder.newInstance()
                .constraint(fixedInForceTimeConstraint).build();
        when(contextMock.getContextData(eq(ContractAgreement.class))).thenReturn(createAgreement("agr1"));

        var result = function.evaluate(Operator.LEQ, Instant.now().plus(ofDays(1)).toString(), permission, contextMock);
    }


    private ContractAgreement createAgreement(String agreementId) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId(UUID.randomUUID().toString())
                .consumerId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }
}