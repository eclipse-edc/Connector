/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.spi.iam.RequestScope;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class DcpCatalogScopeExtractorRegistryTest {

    private final DcpCatalogScopeExtractorRegistry registry = new DcpCatalogScopeExtractorRegistry();

    @Test
    void extractScopes_shouldReturnEmpty_whenNoExtractors() {
        var result = registry.extractScopes(policyContext());

        assertThat(result).isSucceeded().satisfies(scopes -> assertThat(scopes).isEmpty());
    }

    @Test
    void extractScopes_shouldReturnUnionOfAllExtractors() {
        registry.register(ctx -> Set.of("scope1", "scope2"));
        registry.register(ctx -> Set.of("scope2", "scope3"));

        var result = registry.extractScopes(policyContext());

        assertThat(result).isSucceeded().satisfies(scopes -> assertThat(scopes).containsExactlyInAnyOrder("scope1", "scope2", "scope3"));
    }

    @Test
    void extractScopes_shouldPassContextToExtractors() {
        var context = policyContext();
        registry.register(ctx -> ctx == context ? Set.of("scope") : Set.of());

        var result = registry.extractScopes(context);

        assertThat(result).isSucceeded().satisfies(scopes -> assertThat(scopes).containsExactly("scope"));
    }

    @Test
    void extractScopes_shouldFail_whenExtractorThrows() {
        registry.register(ctx -> Set.of("scope1"));
        registry.register(ctx -> {
            throw new RuntimeException("boom");
        });

        var result = registry.extractScopes(policyContext());

        assertThat(result).isFailed().detail().contains("boom");
    }

    private RequestCatalogPolicyContext policyContext() {
        var requestContext = RequestContext.Builder.newInstance()
                .message(CatalogRequestMessage.Builder.newInstance().protocol("protocol").build())
                .direction(RequestContext.Direction.Ingress)
                .participantContextId("participantContextId")
                .build();
        return new RequestCatalogPolicyContext(requestContext, RequestScope.Builder.newInstance());
    }
}
