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

package org.eclipse.edc.iam.decentralizedclaims.spi.scope;

import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

/**
 * Registry for {@link CatalogScopeExtractor}
 */
@ExtensionPoint
public interface CatalogScopeExtractorRegistry {

    /**
     * Register a catalog scope extractor
     *
     * @param extractor The extractor
     */
    void register(CatalogScopeExtractor extractor);

    /**
     * Extract scopes for a catalog request using all the registered {@link CatalogScopeExtractor}s.
     *
     * @param context The policy context of the catalog request
     * @return The union of the extracted scopes if succeeded, otherwise failure
     */
    Result<Set<String>> extractScopes(RequestCatalogPolicyContext context);
}
