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
import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractor;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DynamicScopeExtractorTest {

    private final DcpScopeRegistry registry = mock();

    @Test
    void extractScope_shouldFail_whenRegistryFails() {
        var msg = mock(TransferRequestMessage.class);
        var ctx = RequestContext.Builder.newInstance()
                .direction(RequestContext.Direction.Egress)
                .message(msg)
                .build();

        when(registry.getScopeMapping()).thenReturn(ServiceResult.unexpected("registry-error"));

        var extractor = new DynamicScopeExtractor(registry);

        var result = extractor.extractScopes("any", Operator.EQ, null, ctx);

        assertThat(result).isFailed().detail().contains("Failed to get scope mapping");
    }

    @ParameterizedTest
    @ArgumentsSource(MessageTypeProvider.class)
    void extractScopes(ProtocolRemoteMessage msg) {
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

        var extractor = new DynamicScopeExtractor(registry);

        var result = extractor.extractScopes("pre:resource", Operator.EQ, null, ctx);

        assertThat(result).isSucceeded().satisfies(scopes -> assertThat(scopes).containsOnly("val-wild", "val-match"));
    }

    @Test
    void extractScopes_empty() {
        var msg = mock(TransferRequestMessage.class);
        var ctx = RequestContext.Builder.newInstance()
                .direction(RequestContext.Direction.Egress)
                .message(msg)
                .build();

        when(registry.getScopeMapping()).thenReturn(ServiceResult.success(List.of()));

        ScopeExtractor extractor = new DynamicScopeExtractor(registry);

        var result = extractor.extractScopes(12345, Operator.EQ, null, ctx);

        assertThat(result).isSucceeded().satisfies(scopes -> assertThat(scopes).isEmpty());
    }

    @Test
    void extractScopes_empty_withWrongMessage() {

        var msg = mock(ProtocolRemoteMessage.class);
        var ctx = RequestContext.Builder.newInstance()
                .direction(RequestContext.Direction.Egress)
                .message(msg)
                .build();

        ScopeExtractor extractor = new DynamicScopeExtractor(registry);

        var result = extractor.extractScopes(12345, Operator.EQ, null, ctx);

        assertThat(result).isSucceeded().satisfies(scopes -> assertThat(scopes).isEmpty());
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
