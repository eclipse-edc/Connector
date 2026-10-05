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

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.CatalogScopeExtractor;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.CatalogScopeExtractorRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.RequestScope;
import org.eclipse.edc.spi.monitor.Monitor;

import static java.lang.String.format;

/**
 * DCP pre-validator function for adding scopes to a catalog request using the registered {@link CatalogScopeExtractor}s
 * in the {@link CatalogScopeExtractorRegistry}. The input {@link Policy} is not taken into account, as the extraction
 * is performed on the whole request.
 * <p>
 * A failure in the extraction does not fail the validation, since that would prevent the other validators
 * (e.g. the default scopes mapping) from running: the request proceeds without the additional scopes.
 */
public class CatalogScopeExtractorFunction implements PolicyValidatorRule<RequestCatalogPolicyContext> {

    private final CatalogScopeExtractorRegistry registry;
    private final Monitor monitor;

    public CatalogScopeExtractorFunction(CatalogScopeExtractorRegistry registry, Monitor monitor) {
        this.registry = registry;
        this.monitor = monitor;
    }

    @Override
    public Boolean apply(Policy policy, RequestCatalogPolicyContext context) {
        var params = context.requestScopeBuilder();
        if (params == null) {
            throw new EdcException(format("%s not set in policy context", RequestScope.Builder.class.getName()));
        }
        registry.extractScopes(context)
                .onSuccess(scopes -> scopes.forEach(params::scope))
                .onFailure(failure -> monitor.warning("Failed to extract scopes for catalog request: " + failure.getFailureDetail()));

        return true;
    }
}
