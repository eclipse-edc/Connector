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

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractor;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.RequestScope;
import org.eclipse.edc.spi.monitor.Monitor;

import static java.lang.String.format;

/**
 * DCP pre-validator function for extracting scopes from a {@link Policy} using the registered {@link ScopeExtractor}
 * in the {@link ScopeExtractorRegistry}.
 */
public class DcpScopeExtractorFunction<C extends RequestPolicyContext> implements PolicyValidatorRule<C> {

    private final ScopeExtractorRegistry registry;
    private final Monitor monitor;

    public DcpScopeExtractorFunction(ScopeExtractorRegistry registry, Monitor monitor) {
        this.registry = registry;
        this.monitor = monitor;
    }

    @Override
    public Boolean apply(Policy policy, C context) {
        var params = context.requestScopeBuilder();
        if (params == null) {
            throw new EdcException(format("%s not set in policy context", RequestScope.Builder.class.getName()));
        }
        var results = registry.extractScopes(policy, context).onSuccess(scopes -> scopes.forEach(params::scope));

        if (results.succeeded()) {
            return true;
        } else {
            monitor.warning("Failed to extract scopes from a policy");
            return false;
        }

    }
}
