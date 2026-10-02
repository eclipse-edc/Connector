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

import java.util.Set;

/**
 * Extracts additional DCP scopes for a catalog request (catalog or dataset request message) as a whole.
 * <p>
 * Unlike {@link ScopeExtractor}, which works on the single constraints of a policy, a {@link CatalogScopeExtractor}
 * is invoked once per request, before the token is created (egress) or verified (ingress). On the provider side, this
 * makes it possible to request credentials that are needed later on for evaluating the access policies of the
 * offers in the catalog, which are not known at token verification time.
 * <p>
 * The message and the direction of the request are available through {@link RequestCatalogPolicyContext#requestContext()}.
 * <p>
 * <strong>Note:</strong> on ingress all requested scopes must be satisfied by the counter-party's credentials,
 * otherwise the whole request will be rejected. Implementors should only return scopes that the counter-party is
 * expected to hold, and the counter-party token must authorize them (e.g. with the additional scopes of the catalog
 * request or with a {@link CatalogScopeExtractor} on egress).
 * <p>
 * Extractors can be registered in {@link CatalogScopeExtractorRegistry}.
 */
@FunctionalInterface
public interface CatalogScopeExtractor {

    /**
     * Extract the additional scopes for the catalog request.
     *
     * @param context the policy context of the catalog request
     * @return the set of scopes, never null
     */
    Set<String> extractScopes(RequestCatalogPolicyContext context);
}
