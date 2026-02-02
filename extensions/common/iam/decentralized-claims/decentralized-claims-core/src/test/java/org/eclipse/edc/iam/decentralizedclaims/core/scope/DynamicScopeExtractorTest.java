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

import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractor;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DynamicScopeExtractorTest {

    private final DcpScopeRegistry registry = mock();
    private final RequestPolicyContext context = mock();

    @Test
    void extractScope_shouldFail_whenRegistryFails() {
        var msg = mock(TransferRequestMessage.class);
        var ctx = RequestContext.Builder.newInstance()
                .direction(RequestContext.Direction.Egress)
                .message(msg)
                .build();

        when(context.requestContext()).thenReturn(ctx);
        when(registry.getScopeMapping()).thenReturn(ServiceResult.unexpected("registry-error"));

        var extractor = new DynamicScopeExtractor(registry);

        Set<String> scopes = extractor.extractScopes("any", Operator.EQ, null, context);

        assertTrue(scopes.isEmpty());
        verify(context).reportProblem(contains("Failed to get scope mapping"));
    }

    @ParameterizedTest
    @ArgumentsSource(MessageTypeProvider.class)
    void extractScopes(RemoteMessage msg) {
        // scopes: wildcard (should match), matching profile (should match), non-matching profile (excluded), wrong prefix (excluded)
        var wildcard = DcpScope.Builder.newInstance()
                .id("w")
                .value("val-wild")
                .type(DcpScope.Type.POLICY)
                .prefixMapping("pre:")
                .profile(DcpScope.WILDCARD)
                .build();

        var matching = DcpScope.Builder.newInstance()
                .id("m")
                .value("val-match")
                .type(DcpScope.Type.POLICY)
                .prefixMapping("pre:")
                .profile("proto")
                .build();

        var nonMatchingProfile = DcpScope.Builder.newInstance()
                .id("n1")
                .value("val-non-profile")
                .type(DcpScope.Type.POLICY)
                .prefixMapping("pre:")
                .profile("other")
                .build();

        var wrongPrefix = DcpScope.Builder.newInstance()
                .id("n2")
                .value("val-wrong-prefix")
                .type(DcpScope.Type.POLICY)
                .prefixMapping("other:")
                .profile("proto")
                .build();
        when(msg.getProtocol()).thenReturn("proto");

        var ctx = RequestContext.Builder.newInstance()
                .direction(RequestContext.Direction.Egress)
                .message(msg)
                .build();

        when(registry.getScopeMapping()).thenReturn(ServiceResult.success(List.of(wildcard, matching, nonMatchingProfile, wrongPrefix)));

        when(context.requestContext()).thenReturn(ctx);

        var extractor = new DynamicScopeExtractor(registry);

        var result = extractor.extractScopes("pre:resource", Operator.EQ, null, context);

        assertThat(result).containsOnly("val-wild", "val-match");
    }

    @Test
    void extractScopes_empty() {
        var msg = mock(TransferRequestMessage.class);
        var ctx = RequestContext.Builder.newInstance()
                .direction(RequestContext.Direction.Egress)
                .message(msg)
                .build();

        when(context.requestContext()).thenReturn(ctx);
        when(registry.getScopeMapping()).thenReturn(ServiceResult.success(List.of()));

        ScopeExtractor extractor = new DynamicScopeExtractor(registry);

        Set<String> result = extractor.extractScopes(12345, Operator.EQ, null, context);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractScopes_empty_withWrongMessage() {

        var msg = mock(RemoteMessage.class);
        var ctx = RequestContext.Builder.newInstance()
                .direction(RequestContext.Direction.Egress)
                .message(msg)
                .build();

        when(context.requestContext()).thenReturn(ctx);

        ScopeExtractor extractor = new DynamicScopeExtractor(registry);

        Set<String> result = extractor.extractScopes(12345, Operator.EQ, null, context);
        assertTrue(result.isEmpty());
        verifyNoInteractions(registry);
    }

    public static class MessageTypeProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(mock(TransferRequestMessage.class)),
                    arguments(mock(CatalogRequestMessage.class)),
                    arguments(mock(ContractRequestMessage.class))
            );
        }
    }
}
