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
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

/**
 * Registry for {@link RequestScopeExtractor}s. It owns the context-type dispatch (which extractors apply to a given
 * context) and the combination of their results, replacing the former reliance on the {@code PolicyEngine} validator
 * pipeline and the mutable {@code RequestScope.Builder} side channel.
 */
@ExtensionPoint
public interface RequestScopeExtractorRegistry {

    /**
     * Register an extractor for a given request policy context type. An extractor is invoked for a context when the
     * registered {@code contextType} is assignable from the context's runtime type.
     *
     * @param contextType the request policy context type the extractor applies to.
     * @param extractor   the extractor.
     * @param <C>         the request policy context type.
     */
    <C extends RequestPolicyContext> void registerExtractor(Class<C> contextType, RequestScopeExtractor<C> extractor);

    /**
     * Extract the request scopes using all extractors registered for the context's type, returning the union of their
     * results. Fails as soon as one extractor returns a failure.
     *
     * @param policy  the policy associated with the request.
     * @param context the request policy context.
     * @param <C>     the request policy context type.
     * @return the union of the extracted scopes if all succeeded, otherwise the first failure.
     */
    <C extends RequestPolicyContext> Result<Set<String>> extractScopes(Policy policy, C context);
}
