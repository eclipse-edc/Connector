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

package org.eclipse.edc.policy.context.request.spi;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

/**
 * Extracts the set of DCP request scopes for a request, given its {@link Policy} and {@link RequestPolicyContext}.
 * <p>
 * This replaces the former approach of encoding scope extraction as {@link org.eclipse.edc.policy.engine.spi.PolicyValidatorRule}
 * pre/post validators that mutated a shared {@code RequestScope.Builder} carried inside the context as a side effect.
 * An extractor instead <em>returns</em> the scopes it computes, so the data flow is explicit and the result no longer
 * travels through the context. A failing extraction is reported through the {@link Result} rather than by overloading a
 * {@code Boolean} that also controls policy evaluation.
 * <p>
 * Extractors are registered per {@link RequestPolicyContext} type in the {@link RequestScopeExtractorRegistry}.
 *
 * @param <C> the concrete request policy context type this extractor applies to.
 */
@FunctionalInterface
public interface RequestScopeExtractor<C extends RequestPolicyContext> {

    /**
     * Extract the request scopes.
     *
     * @param policy  the policy associated with the request (may be ignored by request-level extractors).
     * @param context the request policy context.
     * @return the set of scopes if succeeded, otherwise a failure. A blocking failure should fail the whole request;
     *         non-blocking extractors should recover to an empty success instead of returning a failure.
     */
    Result<Set<String>> extractScopes(Policy policy, C context);
}
