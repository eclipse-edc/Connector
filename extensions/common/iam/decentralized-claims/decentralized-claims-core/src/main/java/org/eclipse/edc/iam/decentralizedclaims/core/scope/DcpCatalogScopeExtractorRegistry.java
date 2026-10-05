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
import org.eclipse.edc.spi.result.Result;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class DcpCatalogScopeExtractorRegistry implements CatalogScopeExtractorRegistry {

    private final List<CatalogScopeExtractor> extractors = new CopyOnWriteArrayList<>();

    @Override
    public void register(CatalogScopeExtractor extractor) {
        extractors.add(extractor);
    }

    @Override
    public Result<Set<String>> extractScopes(RequestCatalogPolicyContext context) {
        var scopes = new HashSet<String>();
        for (var extractor : extractors) {
            try {
                scopes.addAll(extractor.extractScopes(context));
            } catch (Exception e) {
                return Result.failure("Failed to extract catalog scopes with %s: %s".formatted(extractor.getClass().getName(), e.getMessage()));
            }
        }
        return Result.success(scopes);
    }
}
