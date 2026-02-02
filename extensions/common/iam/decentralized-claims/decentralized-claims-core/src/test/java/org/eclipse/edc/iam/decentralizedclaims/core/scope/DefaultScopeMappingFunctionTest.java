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

package org.eclipse.edc.iam.decentralizedclaims.core.scope;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.iam.RequestScope;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class DefaultScopeMappingFunctionTest {

    private final RequestPolicyContext context = mock();
    private final DcpScopeRegistry scopeRegistry = mock();

    @Test
    void apply() {
        // prepare default scopes: one wildcard, one matching profile, one non-matching
        var wildcardScope = DcpScope.Builder.newInstance()
                .id("s1")
                .value("v-wild")
                .profile(DcpScope.WILDCARD)
                .build();

        var matchingScope = DcpScope.Builder.newInstance()
                .id("s2")
                .value("v-match")
                .profile("proto")
                .build();

        var nonMatchingScope = DcpScope.Builder.newInstance()
                .id("s3")
                .value("v-non")
                .profile("other")
                .build();


        // existing scopes on request
        var existing = new HashSet<String>();
        existing.add("existing");
        var msg = mock(RemoteMessage.class);
        when(msg.getProtocol()).thenReturn("proto");
        var ctx = RequestContext.Builder.newInstance()
                .direction(RequestContext.Direction.Egress)
                .message(msg)
                .build();
        var builder = RequestScope.Builder.newInstance()
                .scopes(existing);

        when(context.requestContext()).thenReturn(ctx);
        when(context.requestScopeBuilder()).thenReturn(builder);

        // stub the chained calls using deep stubs
        when(scopeRegistry.getDefaultScopes()).thenReturn(ServiceResult.success(List.of(wildcardScope, matchingScope, nonMatchingScope)));

        var fn = new DefaultScopeMappingFunction(scopeRegistry);
        var result = fn.apply(mock(Policy.class), context);
        assertTrue(result);
        var setArg = builder.build().getScopes();
        assertThat(setArg).containsOnly("v-wild", "v-match", "existing");
    }

    @Test
    void apply_shouldReport_whenServiceFails() {
        when(scopeRegistry.getDefaultScopes()).thenReturn(ServiceResult.unexpected("Failed to retrieve default scopes"));

        var fn = new DefaultScopeMappingFunction(scopeRegistry);

        var result = fn.apply(mock(Policy.class), context);

        assertFalse(result);
        verify(context).reportProblem(contains("Failed to retrieve default scopes"));
    }
}