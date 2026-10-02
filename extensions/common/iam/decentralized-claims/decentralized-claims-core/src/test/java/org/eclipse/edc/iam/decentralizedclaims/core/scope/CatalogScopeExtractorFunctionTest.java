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

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.CatalogScopeExtractorRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.RequestScope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogScopeExtractorFunctionTest {

    private final CatalogScopeExtractorRegistry registry = mock();
    private final RequestCatalogPolicyContext policyContext = mock();
    private final Monitor monitor = mock();
    private final CatalogScopeExtractorFunction function = new CatalogScopeExtractorFunction(registry, monitor);

    @Test
    void apply_shouldAddScopes() {
        var scopeBuilder = RequestScope.Builder.newInstance().scope("existing");
        when(policyContext.requestScopeBuilder()).thenReturn(scopeBuilder);
        when(registry.extractScopes(policyContext)).thenReturn(Result.success(Set.of("scope1", "scope2")));

        assertThat(function.apply(Policy.Builder.newInstance().build(), policyContext)).isTrue();
        assertThat(scopeBuilder.build().getScopes()).containsExactlyInAnyOrder("existing", "scope1", "scope2");
    }

    @Test
    void apply_shouldNotFail_whenExtractionFails() {
        var scopeBuilder = RequestScope.Builder.newInstance().scope("existing");
        when(policyContext.requestScopeBuilder()).thenReturn(scopeBuilder);
        when(registry.extractScopes(policyContext)).thenReturn(Result.failure("failure"));

        assertThat(function.apply(Policy.Builder.newInstance().build(), policyContext)).isTrue();
        assertThat(scopeBuilder.build().getScopes()).containsExactly("existing");
        verify(monitor).warning(anyString());
    }

    @Test
    void apply_shouldThrow_whenScopeBuilderMissing() {
        assertThatThrownBy(() -> function.apply(Policy.Builder.newInstance().build(), policyContext)).isInstanceOf(EdcException.class);
    }
}
