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

package org.eclipse.edc.iam.identitytrust.core.scope;

import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractor;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Failure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.core.scope.ScopeTestFunctions.andConstraint;
import static org.eclipse.edc.iam.identitytrust.core.scope.ScopeTestFunctions.atomicConstraint;
import static org.eclipse.edc.iam.identitytrust.core.scope.ScopeTestFunctions.dutyPolicy;
import static org.eclipse.edc.iam.identitytrust.core.scope.ScopeTestFunctions.orConstraint;
import static org.eclipse.edc.iam.identitytrust.core.scope.ScopeTestFunctions.permissionPolicy;
import static org.eclipse.edc.iam.identitytrust.core.scope.ScopeTestFunctions.prohibitionPolicy;
import static org.eclipse.edc.iam.identitytrust.core.scope.ScopeTestFunctions.xoneConstraint;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DcpScopeExtractorRegistryTest {

    private final RequestPolicyContext ctx = mock();
    private final ScopeExtractor extractor = mock();
    private final DcpScopeExtractorRegistry registry = new DcpScopeExtractorRegistry();

    @BeforeEach
    void setup() {
        registry.registerScopeExtractor(extractor);
    }

    @ParameterizedTest
    @ArgumentsSource(SingleConstraintProvider.class)
    void extractScopes(Policy policy, ConstraintData data) {

        when(extractor.extractScopes(data.left(), data.operator(), data.right, ctx)).thenReturn(Set.of(data.left()));

        assertThat(registry.extractScopes(policy, ctx))
                .isSucceeded()
                .satisfies(scopes -> assertThat(scopes).contains(data.left));

        verify(extractor).extractScopes(data.left(), data.operator(), data.right(), ctx);
    }

    @ParameterizedTest
    @ArgumentsSource(MultipleConstraintProvider.class)
    void extractScopes_withMultipleConstraints(Policy policy, List<ConstraintData> constraintData) {

        var scopes = constraintData.stream().map(ConstraintData::left).collect(Collectors.toSet());

        constraintData.forEach(data -> when(extractor.extractScopes(data.left(), data.operator(), data.right, ctx)).thenReturn(Set.of(data.left())));


        assertThat(registry.extractScopes(policy, ctx))
                .isSucceeded()
                .satisfies(returnedScopes -> assertThat(returnedScopes).containsAll(scopes));

        constraintData.forEach(data -> verify(extractor).extractScopes(data.left(), data.operator(), data.right(), ctx));
    }

    @Test
    void extractScopes_fails_whenContextProblem() {

        var left = "left";
        var right = "right";
        var operator = Operator.EQ;
        var problems = List.of("problem");
        when(extractor.extractScopes(left, operator, right, ctx)).thenReturn(Set.of());
        when(ctx.hasProblems()).thenReturn(true);
        when(ctx.getProblems()).thenReturn(problems);

        var policy = permissionPolicy(atomicConstraint(left, operator, right));

        assertThat(registry.extractScopes(policy, ctx))
                .isFailed()
                .extracting(Failure::getMessages)
                .isEqualTo(problems);

        verify(extractor).extractScopes(left, operator, right, ctx);
    }

    private record ConstraintData(String left, Operator operator, Object right) {
    }

    private static class SingleConstraintProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            var left = "left";
            var right = "right";
            var operator = Operator.EQ;
            var constraint = atomicConstraint(left, operator, right);
            var constraintData = new ConstraintData(left, operator, right);
            return Stream.of(
                    of(permissionPolicy(constraint), constraintData),
                    of(dutyPolicy(constraint), constraintData),
                    of(prohibitionPolicy(constraint), constraintData)
            );
        }
    }

    private static class MultipleConstraintProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            var operator = Operator.EQ;
            var data = IntStream.range(0, 5).mapToObj(i -> new ConstraintData("left" + i, operator, "right" + i)).toList();

            var constraints = data.stream().map(d -> atomicConstraint(d.left(), operator, d.right()))
                    .map(Constraint.class::cast).toList();

            return Stream.of(
                    of(permissionPolicy(andConstraint(constraints)), data),
                    of(permissionPolicy(orConstraint(constraints)), data),
                    of(permissionPolicy(xoneConstraint(constraints)), data)
            );
        }
    }


}
