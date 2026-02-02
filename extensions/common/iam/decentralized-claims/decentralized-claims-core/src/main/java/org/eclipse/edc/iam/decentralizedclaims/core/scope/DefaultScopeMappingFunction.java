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
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.model.Policy;

import java.util.HashSet;

import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.WILDCARD;

/**
 * A policy validator rule that adds default DCP scopes to the request context based on the profile.
 */
public class DefaultScopeMappingFunction implements PolicyValidatorRule<RequestPolicyContext> {
    private final DcpScopeRegistry scopeRegistry;

    public DefaultScopeMappingFunction(DcpScopeRegistry scopeRegistry) {
        this.scopeRegistry = scopeRegistry;
    }

    @Override
    public Boolean apply(Policy policy, RequestPolicyContext context) {
        var defaultScopes = scopeRegistry.getDefaultScopes();
        if (defaultScopes.failed()) {
            context.reportProblem("Failed to retrieve default scopes: " + defaultScopes.getFailureMessages());
            return false;
        }
        var defaultScopeList = defaultScopes.getContent().stream()
                .filter(scope -> filterScope(scope, context))
                .map(DcpScope::getValue).toList();
        var requestScopeBuilder = context.requestScopeBuilder();
        var rq = requestScopeBuilder.build();
        var existingScope = rq.getScopes();
        var newScopes = new HashSet<>(defaultScopeList);
        newScopes.addAll(existingScope);
        requestScopeBuilder.scopes(newScopes);
        return true;
    }


    boolean filterScope(DcpScope scope, RequestPolicyContext context) {
        return scope.getProfile().equals(WILDCARD) || scope.getProfile().equals(context.requestContext().getMessage().getProtocol());
    }
}
