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

package org.eclipse.edc.iam.decentralizedclaims.core.scope;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.iam.RequestScope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DcpScopeExtractorFunctionTest {

    private final ScopeExtractorRegistry registry = mock();
    private final TestRequestPolicyContext policyContext = mock();
    private DcpScopeExtractorFunction<TestRequestPolicyContext> function;

    @BeforeEach
    void setup() {
        function = new DcpScopeExtractorFunction<>(registry, mock(Monitor.class));
    }

    @Test
    void apply() {
        var policy = Policy.Builder.newInstance().build();
        var scopeBuilder = RequestScope.Builder.newInstance();

        when(policyContext.requestScopeBuilder()).thenReturn(scopeBuilder);
        when(registry.extractScopes(eq(policy), any())).thenReturn(Result.success(Set.of("scope1", "scope2")));

        assertThat(function.apply(policy, policyContext)).isTrue();
        assertThat(scopeBuilder.build())
                .extracting(RequestScope::getScopes)
                .satisfies(scopes -> assertThat(scopes).contains("scope1", "scope2"));
    }

    @Test
    void apply_shouldThrow_whenTokenBuilderMissing() {
        var policy = Policy.Builder.newInstance().build();

        when(registry.extractScopes(eq(policy), any())).thenReturn(Result.success(Set.of("scope1", "scope2")));

        assertThatThrownBy(() -> function.apply(policy, policyContext)).isInstanceOf(EdcException.class);
    }

    @Test
    void apply_fail_whenScopeExtractorFails() {
        var policy = Policy.Builder.newInstance().build();
        var scopeBuilder = RequestScope.Builder.newInstance();
        when(policyContext.requestScopeBuilder()).thenReturn(scopeBuilder);

        when(registry.extractScopes(eq(policy), any())).thenReturn(Result.failure("failure"));

        assertThat(function.apply(policy, policyContext)).isFalse();
    }

    private static class TestRequestPolicyContext extends RequestPolicyContext {

        protected TestRequestPolicyContext(RequestContext requestContext, RequestScope.Builder requestScopeBuilder) {
            super(requestContext, requestScopeBuilder);
        }

        @Override
        public String scope() {
            return "request.test";
        }
    }

}
