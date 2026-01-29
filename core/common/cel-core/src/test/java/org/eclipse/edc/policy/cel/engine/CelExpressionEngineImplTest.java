/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.policy.cel.engine;

import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CelExpressionEngineImplTest {
    private final CelExpressionStore store = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final CelExpressionEngineImpl registry = new CelExpressionEngineImpl(transactionContext, store, mock());


    @Test
    void evaluateExpression_noMatching() {
        when(store.query(any())).thenReturn(List.of());

        Map<String, Object> claims = Map.of("agent", Map.of("id", "agent-123"));
        var result = registry.evaluateExpression("test", Operator.EQ, "null", claims);

        assertThat(result).isFailed();
    }

    @Test
    void evaluateExpression_invalidExpression() {
        when(store.query(any())).thenReturn(List.of(expression("invalid expression")));

        Map<String, Object> claims = Map.of("agent", Map.of("id", "agent-123"));
        var result = registry.evaluateExpression("test", Operator.EQ, "null", claims);

        assertThat(result).isFailed();
    }

    @Test
    void evaluateExpression_simpleExpression() {
        when(store.query(any())).thenReturn(List.of(expression("ctx.agent.id == 'agent-123'")));


        Map<String, Object> claims = Map.of("agent", Map.of("id", "agent-123"));
        var result = registry.evaluateExpression("test", Operator.EQ, "null", claims);

        assertThat(result).isSucceeded();
    }

    @Test
    void evaluateExpression_credential() {

        var params = createParams("agent-123");

        var expression = """
                ctx.agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now))
                """;

        when(store.query(any())).thenReturn(List.of(expression(expression)));

        var result = registry.evaluateExpression("test", Operator.EQ, "null", params);

        assertThat(result).isSucceeded();
    }

    @Test
    void evaluateExpression_credential_withMultipleConditions() {

        var params = createParams("agent-123");

        var expression = """
                ctx.agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now && cs.membershipType == 'gold'))
                """;

        when(store.query(any())).thenReturn(List.of(expression(expression)));

        var result = registry.evaluateExpression("test", Operator.EQ, "null", params);

        assertThat(result).isSucceeded();
    }

    @Test
    void evaluateExpression_whenMissingKeys() {

        var params = createParams("agent-123");

        var expression = """
                ctx.agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now && cs.missingClaim == 'gold'))
                """;

        when(store.query(any())).thenReturn(List.of(expression(expression)));

        var result = registry.evaluateExpression("test", Operator.EQ, "null", params);

        assertThat(result).isFailed();
    }


    @ParameterizedTest
    @ArgumentsSource(InputProvider.class)
    void evaluateExpression_withRightOperand(String rightOperand, boolean expectedEvaluation) {

        var params = createParams("agent-123");

        var expression = """
                ctx.agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) > timestamp(this.rightOperand)))
                """;

        when(store.query(any())).thenReturn(List.of(expression(expression)));

        var result = registry.evaluateExpression("test", Operator.EQ, rightOperand, params);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isEqualTo(expectedEvaluation);
    }

    @Test
    void validate() {

        var expression = """
                ctx.agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now && cs.missingClaim == 'gold'))
                """;


        var result = registry.validate(expression);

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_withInvalidVariables() {

        var expression = """
                agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now))
                """;


        var result = registry.validate(expression);

        assertThat(result).isFailed();
    }

    @Test
    void test() {

        var expression = """
                ctx.agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now))
                """;


        var result = registry.test(expression, "leftOperand", Operator.EQ, "rightOperand", createParams("agent-123"));

        assertThat(result).isSucceeded();
    }

    @Test
    void test_withMissingKeys() {

        var expression = """
                ctx.agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now && cs.missingClaim == 'gold'))
                """;


        var result = registry.test(expression, "leftOperand", Operator.EQ, "rightOperand", createParams("agent-123"));

        assertThat(result).isFailed();
    }

    @Test
    void canEvaluate() {
        when(store.query(any())).thenReturn(List.of(expression("empty")));

        var result = registry.canEvaluate("leftOperand");

        assertThat(result).isTrue();
    }

    @Test
    void canEvaluate_noMatching() {
        when(store.query(any())).thenReturn(List.of());

        var result = registry.canEvaluate("leftOperand");

        assertThat(result).isFalse();
    }

    private CelExpression expression(String expr) {
        return CelExpression.Builder.newInstance().id("id")
                .leftOperand("test")
                .expression(expr)
                .description("description")
                .build();
    }

    private @NotNull Map<String, Object> credential() {
        return Map.of(
                "id", "credential-456",
                "type", List.of("VerifiableCredential", "MembershipCredential"),
                "credentialSubject", List.of(Map.of(
                        "id", "subject-789",
                        "membershipStartDate", "2023-01-01T00:00:00Z",
                        "membershipType", "gold"
                )));
    }

    private Map<String, Object> createParams(String id) {
        return Map.of("agent", createAgentParams(id));
    }

    private @NotNull Map<String, Object> createAgentParams(String id) {

        return Map.of("id", id,
                "claims", Map.of("vc", List.of(credential())));

    }


    private static class InputProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments("2022-01-01T00:00:00Z", true),
                    arguments("2024-01-01T00:00:00Z", false)
            );
        }
    }


}
