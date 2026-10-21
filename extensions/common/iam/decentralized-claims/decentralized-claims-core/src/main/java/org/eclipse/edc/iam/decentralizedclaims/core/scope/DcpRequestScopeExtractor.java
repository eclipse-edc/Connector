/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core.scope;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractor;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.spi.result.Result;

import java.util.HashSet;
import java.util.Set;

import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.WILDCARD;

/**
 * DCP request scope extractor. Merges the responsibilities of the former {@code DcpScopeExtractorFunction}
 * (policy-constraint scopes, via the {@link ScopeExtractorRegistry}) and {@code DefaultScopeMappingFunction}
 * (profile-filtered default scopes, via the {@link DcpScopeRegistry}) into a single extractor that returns the union
 * of both.
 */
public class DcpRequestScopeExtractor implements RequestScopeExtractor {

    private final ScopeExtractorRegistry scopeExtractorRegistry;
    private final DcpScopeRegistry dcpScopeRegistry;

    public DcpRequestScopeExtractor(ScopeExtractorRegistry scopeExtractorRegistry, DcpScopeRegistry dcpScopeRegistry) {
        this.scopeExtractorRegistry = scopeExtractorRegistry;
        this.dcpScopeRegistry = dcpScopeRegistry;
    }

    @Override
    public Result<Set<String>> extractScopes(Policy policy, RequestContext context) {
        var scopes = new HashSet<String>();

        var constraintScopes = scopeExtractorRegistry.extractScopes(policy, context);
        if (constraintScopes.failed()) {
            return Result.failure(constraintScopes.getFailureMessages());
        }
        scopes.addAll(constraintScopes.getContent());

        // default, profile-filtered scopes (formerly DefaultScopeMappingFunction): a failure here is blocking
        var defaultScopes = dcpScopeRegistry.getDefaultScopes();
        if (defaultScopes.failed()) {
            return Result.failure("Failed to retrieve default scopes: " + defaultScopes.getFailureMessages());
        }
        defaultScopes.getContent().stream()
                .filter(scope -> filterScope(scope, context))
                .map(DcpScope::getValue)
                .forEach(scopes::add);

        return Result.success(scopes);
    }

    private boolean filterScope(DcpScope scope, RequestContext context) {
        return scope.getProfile().equals(WILDCARD) || scope.getProfile().equals(context.getMessage().getProtocol());
    }
}
