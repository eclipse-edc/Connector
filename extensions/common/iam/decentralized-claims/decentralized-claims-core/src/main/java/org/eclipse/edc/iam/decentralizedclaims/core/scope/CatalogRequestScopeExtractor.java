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

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.CatalogScopeExtractorRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractor;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

/**
 * Request scope extractor for catalog requests (replaces the former {@code CatalogScopeExtractorFunction}). It ignores
 * the policy and extracts request-level scopes via the {@link CatalogScopeExtractorRegistry}.
 * <p>
 * A failure in the extraction is non-blocking: it is logged and the request proceeds without the additional scopes,
 * preserving the behaviour of the former validator (which returned {@code true} so the other validators could still
 * run).
 */
public class CatalogRequestScopeExtractor implements RequestScopeExtractor<RequestCatalogPolicyContext> {

    private final CatalogScopeExtractorRegistry registry;
    private final Monitor monitor;

    public CatalogRequestScopeExtractor(CatalogScopeExtractorRegistry registry, Monitor monitor) {
        this.registry = registry;
        this.monitor = monitor;
    }

    @Override
    public Result<Set<String>> extractScopes(Policy policy, RequestCatalogPolicyContext context) {
        var result = registry.extractScopes(context);
        if (result.failed()) {
            monitor.warning("Failed to extract scopes for catalog request: " + result.getFailureDetail());
            return Result.success(Set.of());
        }
        return Result.success(result.getContent());
    }
}
