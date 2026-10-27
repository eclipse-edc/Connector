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

import org.eclipse.edc.policy.cel.function.CelFunction;
import org.eclipse.edc.policy.cel.function.CelFunctionRegistry;
import org.eclipse.edc.policy.cel.function.CelFunctionRegistryImpl;
import org.eclipse.edc.policy.cel.function.CelValueType;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CelExpressionEngineImplTest {
    private final CelExpressionStore store = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final CelFunctionRegistry functionRegistry = new CelFunctionRegistryImpl();
    private final CelExpressionEngineImpl registry = new CelExpressionEngineImpl(transactionContext, store, mock(), functionRegistry);


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
    void evaluateExpression_shouldEvaluateEachCallWithItsOwnParams_whenExpressionReused() {
        when(store.query(any())).thenReturn(List.of(expression("ctx.agent.id == 'agent-123' && this.rightOperand == 'right'")));

        assertThat(registry.evaluateExpression("test", Operator.EQ, "right", createParams("agent-123"))).isSucceeded().isEqualTo(true);
        assertThat(registry.evaluateExpression("test", Operator.EQ, "right", createParams("agent-456"))).isSucceeded().isEqualTo(false);
        assertThat(registry.evaluateExpression("test", Operator.EQ, "other", createParams("agent-123"))).isSucceeded().isEqualTo(false);
        assertThat(registry.evaluateExpression("test", Operator.EQ, "right", createParams("agent-123"))).isSucceeded().isEqualTo(true);
    }

    @Test
    void evaluateExpression_shouldUseUpdatedExpression() {
        when(store.query(any()))
                .thenReturn(List.of(expression("ctx.agent.id == 'agent-123'")))
                .thenReturn(List.of(expression("ctx.agent.id == 'agent-456'")));

        assertThat(registry.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"))).isSucceeded().isEqualTo(true);
        assertThat(registry.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"))).isSucceeded().isEqualTo(false);
    }

    @Test
    void evaluateExpression_shouldFailEveryTime_whenExpressionInvalid() {
        when(store.query(any())).thenReturn(List.of(expression("invalid expression")));

        assertThat(registry.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"))).isFailed();
        assertThat(registry.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"))).isFailed();
    }

    @Test
    void evaluateExpression_whenOperatorSupported() {
        when(store.query(any())).thenReturn(List.of(expressionBuilder("true").supportedOperators(Set.of(Operator.EQ)).build()));

        var result = registry.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"));

        assertThat(result).isSucceeded().isEqualTo(true);
    }

    @Test
    void evaluateExpression_whenOperatorNotSupported_shouldFail() {
        when(store.query(any())).thenReturn(List.of(expressionBuilder("true").supportedOperators(Set.of(Operator.EQ)).build()));

        var result = registry.evaluateExpression("test", Operator.NEQ, "null", createParams("agent-123"));

        assertThat(result).isFailed().detail().contains("NEQ");
    }

    @Test
    void evaluateExpression_shouldSkipExpressionsNotSupportingOperator() {
        when(store.query(any())).thenReturn(List.of(
                expressionBuilder("false").supportedOperators(Set.of(Operator.IS_PART_OF)).build(),
                expressionBuilder("true").supportedOperators(Set.of(Operator.EQ)).build(),
                expression("true")));

        var result = registry.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"));

        assertThat(result).isSucceeded().isEqualTo(true);
    }

    @Test
    void evaluateExpression_shouldTreatInAsIsPartOf() {
        when(store.query(any())).thenReturn(List.of(expressionBuilder("this.operator == 'IN'").supportedOperators(Set.of(Operator.IS_PART_OF)).build()));

        var result = registry.evaluateExpression("test", Operator.IN, "null", createParams("agent-123"));

        assertThat(result).isSucceeded().isEqualTo(true);
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

    @ParameterizedTest
    @ArgumentsSource(OperatorProvider.class)
    void evaluateExpression_withOperator(Operator operator, String expectedName, String expectedOdrlName) {
        var expression = "this.operator == '%s' && this.odrlOperator == '%s'".formatted(expectedName, expectedOdrlName);
        when(store.query(any())).thenReturn(List.of(expression(expression)));

        var result = registry.evaluateExpression("test", operator, "null", createParams("agent-123"));

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isTrue();
    }

    /**
     * The CEL environment must be built lazily:extensions register their functions during initialization, which
     * happens after the engine is constructed — as it is here, in the field initializer.
     */
    @Test
    void evaluateExpression_withCustomFunctionRegisteredAfterConstruction() {
        functionRegistry.registerFunction(new CelFunction("hasCredential", "test_has_credential", true,
                CelValueType.BOOL, List.of(CelValueType.LIST, CelValueType.STRING),
                args -> ((List<?>) args.get(0)).stream()
                        .anyMatch(c -> ((List<?>) ((Map<?, ?>) c).get("type")).contains(args.get(1)))));

        when(store.query(any())).thenReturn(List.of(expression("ctx.agent.claims.vc.hasCredential('MembershipCredential')")));

        var result = registry.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"));

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isTrue();
    }

    /**
     * Once the environment is built the registry is sealed, so a function registered too late fails loudly rather
     * than being silently missing from expressions.
     */
    @Test
    void registerFunction_afterEnvironmentBuilt_throws() {
        registry.validate("ctx.agent.id == 'x'");

        assertThatThrownBy(() -> functionRegistry.registerFunction(new CelFunction("tooLate", "test_too_late", true,
                CelValueType.BOOL, List.of(CelValueType.LIST), args -> true)))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("tooLate");
    }

    @Test
    void validate_whenCustomFunctionNotRegistered_fails() {
        var result = registry.validate("ctx.agent.claims.vc.notRegistered('x')");

        assertThat(result).isFailed();
    }

    /**
     * The compiler pins the result type to bool, so an expression returning the filtered list must not type-check.
     */
    @Test
    void validate_whenCustomFunctionReturnsNonBoolean_fails() {
        functionRegistry.registerFunction(new CelFunction("withType", "test_with_type", true,
                CelValueType.LIST, List.of(CelValueType.LIST, CelValueType.STRING), args -> List.of()));

        assertThat(registry.validate("ctx.agent.claims.vc.withType('X')")).isFailed();
        assertThat(registry.validate("ctx.agent.claims.vc.withType('X').size() > 0")).isSucceeded();
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

        assertThat(registry.canEvaluate("test")).isTrue();
        assertThat(registry.canEvaluate("other")).isFalse();
    }

    @Test
    void canEvaluate_withOperator() {
        when(store.query(any())).thenReturn(List.of(
                expressionBuilder("true").supportedOperators(Set.of(Operator.EQ)).build(),
                expressionBuilder("true").supportedOperators(Set.of(Operator.IS_PART_OF)).build()));

        assertThat(registry.canEvaluate("test", Operator.EQ)).isTrue();
        assertThat(registry.canEvaluate("test", Operator.IN)).isTrue();
        assertThat(registry.canEvaluate("test", Operator.NEQ)).isFalse();
    }

    @Test
    void canEvaluate_withOperator_whenNoneConfigured() {
        when(store.query(any())).thenReturn(List.of(expression("true")));

        assertThat(registry.canEvaluate("test", Operator.NEQ)).isTrue();
    }

    @Test
    void canEvaluate_withOperator_noMatching() {
        when(store.query(any())).thenReturn(List.of());

        assertThat(registry.canEvaluate("test", Operator.EQ)).isFalse();
    }

    @Test
    void canEvaluate_noMatching() {
        when(store.query(any())).thenReturn(List.of());

        var result = registry.canEvaluate("leftOperand");

        assertThat(result).isFalse();
    }

    @Test
    void evaluationScopes() {
        var expr = expressionBuilder("empty").scopes(Set.of("scope1", "scope2")).build();
        when(store.query(any())).thenReturn(List.of(expr));

        var result = registry.evaluationScopes("test");

        assertThat(result).containsAll(expr.getScopes());
    }

    @Test
    void evaluationScopes_shouldIncludeExpressionsBoundToAction() {
        var expr = expressionBuilder("empty").leftOperand("other").actions(Set.of("use")).scopes(Set.of("scope1")).build();
        when(store.query(any())).thenReturn(List.of(expr));

        assertThat(registry.evaluationScopes("use")).containsExactly("scope1");
        assertThat(registry.evaluationScopes("test")).isEmpty();
    }

    @Nested
    class ExpressionsCache {

        private final MutableClock clock = new MutableClock();
        private final CelExpressionEngineImpl cachingEngine = new CelExpressionEngineImpl(transactionContext, store, mock(), functionRegistry, clock, 1000);

        @Test
        void shouldReadExpressionsOnce_withinTtl() {
            when(store.query(any())).thenReturn(List.of(expression("ctx.agent.id == 'agent-123'")));

            assertThat(cachingEngine.canEvaluate("test")).isTrue();
            assertThat(cachingEngine.evaluationScopes("test")).isNotNull();
            assertThat(cachingEngine.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"))).isSucceeded().isEqualTo(true);

            verify(store, times(1)).query(QuerySpec.max());
        }

        @Test
        void shouldReadExpressionsAgain_whenTtlExpired() {
            when(store.query(any()))
                    .thenReturn(List.of(expression("ctx.agent.id == 'agent-123'")))
                    .thenReturn(List.of(expression("ctx.agent.id == 'agent-456'")));

            assertThat(cachingEngine.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"))).isSucceeded().isEqualTo(true);
            clock.advance(999);
            assertThat(cachingEngine.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"))).isSucceeded().isEqualTo(true);
            clock.advance(1);
            assertThat(cachingEngine.evaluateExpression("test", Operator.EQ, "null", createParams("agent-123"))).isSucceeded().isEqualTo(false);

            verify(store, times(2)).query(any());
        }

        @Test
        void shouldReadExpressionsAgain_whenInvalidated() {
            when(store.query(any()))
                    .thenReturn(List.of())
                    .thenReturn(List.of(expression("true")));

            assertThat(cachingEngine.canEvaluate("test")).isFalse();
            cachingEngine.invalidate();
            assertThat(cachingEngine.canEvaluate("test")).isTrue();
        }
    }

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private CelExpression expression(String expr) {
        return expressionBuilder(expr).build();
    }

    private CelExpression.Builder expressionBuilder(String expr) {
        return CelExpression.Builder.newInstance().id("id")
                .leftOperand("test")
                .expression(expr)
                .description("description");
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

    private static class OperatorProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(Operator.EQ, "EQ", "eq"),
                    arguments(Operator.GEQ, "GEQ", "gteq"),
                    arguments(Operator.IS_PART_OF, "IS_PART_OF", "isPartOf"),
                    arguments(Operator.IN, "IN", "isPartOf"),
                    arguments(Operator.IS_ANY_OF, "IS_ANY_OF", "isAnyOf")
            );
        }
    }


}
