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

package org.eclipse.edc.connector.controlplane.policy.contract;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.policy.engine.PolicyEngineImpl;
import org.eclipse.edc.policy.engine.RuleBindingRegistryImpl;
import org.eclipse.edc.policy.engine.ScopeFilter;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.engine.validation.RuleValidator;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.Duration.ofDays;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static org.eclipse.edc.connector.controlplane.policy.contract.ContractExpiryCheckFunction.CONTRACT_EXPIRY_EVALUATION_KEY;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.GEQ;
import static org.eclipse.edc.policy.model.Operator.GT;
import static org.eclipse.edc.policy.model.Operator.LEQ;
import static org.eclipse.edc.policy.model.Operator.LT;
import static org.eclipse.edc.policy.model.Operator.NEQ;
import static org.junit.jupiter.params.provider.Arguments.of;

@ComponentTest
class ContractExpiryCheckFunctionEvaluationTest {
    private static final String TRANSFER_SCOPE = "transfer.process";

    private static final Instant NOW = now();
    private final ContractExpiryCheckFunction function = new ContractExpiryCheckFunction();
    private final RuleBindingRegistry bindingRegistry = new RuleBindingRegistryImpl();
    private PolicyEngine policyEngine;

    @BeforeEach
    void setup() {
        // bind/register rule to evaluate contract expiry
        bindingRegistry.bind("use", TRANSFER_SCOPE);
        bindingRegistry.bind(CONTRACT_EXPIRY_EVALUATION_KEY, TRANSFER_SCOPE);
        policyEngine = new PolicyEngineImpl(new ScopeFilter(bindingRegistry), new RuleValidator(bindingRegistry));
        policyEngine.registerFunction(TRANSFER_SCOPE, Permission.class, CONTRACT_EXPIRY_EVALUATION_KEY, function);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidTimeProvider.class)
    void evaluate_fixed_isValid(Operator startOp, Instant start, Operator endOp, Instant end) {
        var policy = createInForcePolicy(startOp, start, endOp, end);
        var context = PolicyContextImpl.Builder.newInstance().additional(Instant.class, NOW).build();

        var result = policyEngine.evaluate(TRANSFER_SCOPE, policy, context);

        AbstractResultAssert.assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidTimeProvider.class)
    void evaluate_fixed_isInvalid(Operator startOp, Instant start, Operator endOp, Instant end) {
        var policy = createInForcePolicy(startOp, start, endOp, end);
        var context = PolicyContextImpl.Builder.newInstance().additional(Instant.class, NOW).build();

        var result = policyEngine.evaluate(TRANSFER_SCOPE, policy, context);

        AbstractResultAssert.assertThat(result)
                .isFailed()
                .detail().contains(CONTRACT_EXPIRY_EVALUATION_KEY);
    }

    @ParameterizedTest
    @ValueSource(strings = { "100d", "25h", "2m", "67s" })
    void evaluate_durationAsEnd_isValid(String numeric) {
        var policy = createInForcePolicy(GEQ, NOW.minusSeconds(60), LEQ, "contractAgreement+" + numeric);
        var context = PolicyContextImpl.Builder.newInstance()
                .additional(Instant.class, NOW)
                .additional(ContractAgreement.class, createAgreement("test-agreement", NOW))
                .build();

        var result = policyEngine.evaluate(TRANSFER_SCOPE, policy, context);

        AbstractResultAssert.assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = { "-100d", "-25h", "-2m", "-67s" })
    void evaluate_durationAsStart_isValid(String numeric) {
        var policy = createInForcePolicy(GEQ, "contractAgreement+" + numeric, LEQ, NOW.plusSeconds(60));
        var context = PolicyContextImpl.Builder.newInstance()
                .additional(Instant.class, NOW)
                .additional(ContractAgreement.class, createAgreement("test-agreement", NOW))
                .build();

        var result = policyEngine.evaluate(TRANSFER_SCOPE, policy, context);

        AbstractResultAssert.assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = { "d", "*25h", "2ms" })
    void evaluate_duration_invalidExpression(String numeric) {
        var policy = createInForcePolicy(GEQ, "contractAgreement+" + numeric, LEQ, NOW.plusSeconds(60));
        var context = PolicyContextImpl.Builder.newInstance()
                .additional(Instant.class, NOW)
                .additional(ContractAgreement.class, createAgreement("test-agreement", NOW))
                .build();

        var result = policyEngine.evaluate(TRANSFER_SCOPE, policy, context);

        AbstractResultAssert.assertThat(result).isFailed();
    }

    private Policy createInForcePolicy(Operator operatorStart, Object startDate, Operator operatorEnd, Object endDate) {
        var fixedInForceTimeConstraint = AndConstraint.Builder.newInstance()
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression(CONTRACT_EXPIRY_EVALUATION_KEY))
                        .operator(operatorStart)
                        .rightExpression(new LiteralExpression(startDate.toString()))
                        .build())
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression(CONTRACT_EXPIRY_EVALUATION_KEY))
                        .operator(operatorEnd)
                        .rightExpression(new LiteralExpression(endDate.toString()))
                        .build())
                .build();
        var permission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .constraint(fixedInForceTimeConstraint).build();

        return Policy.Builder.newInstance()
                .permission(permission)
                .build();
    }

    private ContractAgreement createAgreement(String agreementId) {
        return createAgreement(agreementId, NOW);
    }

    private ContractAgreement createAgreement(String agreementId, Instant signingTime) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId(UUID.randomUUID().toString())
                .consumerId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .contractSigningDate(signingTime.getEpochSecond())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private static class ValidTimeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    of(GEQ, NOW.minus(ofDays(1)), LEQ, NOW.plus(ofDays(1))),
                    of(GEQ, NOW.minus(ofDays(1)), LEQ, NOW.plus(ofDays(1))),
                    of(GEQ, NOW, LEQ, NOW.plus(ofDays(1))),
                    of(GEQ, NOW.minus(ofDays(1)), LEQ, NOW),
                    of(GT, NOW.minus(ofSeconds(1)), LT, NOW.plusSeconds(1L)),
                    of(EQ, NOW, LT, NOW.plusSeconds(1)),
                    of(GEQ, NOW.minusSeconds(1), EQ, NOW),
                    of(NEQ, NOW.minusSeconds(4), LT, NOW.plusSeconds(10))
            );
        }
    }

    private static class InvalidTimeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    of(NEQ, NOW, LEQ, NOW.plusSeconds(1)), //lower bound violation
                    of(GEQ, NOW, NEQ, NOW), // upper bound violation
                    of(GEQ, NOW.plusSeconds(1), LEQ, NOW.plusSeconds(10)), //NOW is before start
                    of(GEQ, NOW.minusSeconds(30), LEQ, NOW.minusSeconds(10)), //NOW is after  end
                    of(GT, NOW, LEQ, NOW.plusSeconds(40)), // lower bound violation, NOW is exactly on start
                    of(GT, NOW.minusSeconds(10), LT, NOW), // upper bound violation, NOW is exactly on end
                    of(NEQ, NOW, LEQ, NOW.plusSeconds(30)) //start cannot be NOW, but it is
            );
        }
    }
}
