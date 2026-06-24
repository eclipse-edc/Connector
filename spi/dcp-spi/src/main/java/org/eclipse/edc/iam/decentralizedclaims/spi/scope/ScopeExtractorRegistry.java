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

package org.eclipse.edc.iam.decentralizedclaims.spi.scope;

import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

/**
 * Registry for {@link ScopeExtractor}
 */
@ExtensionPoint
public interface ScopeExtractorRegistry {

    /**
     * Register a scope extractor
     *
     * @param extractor The extractor
     */
    void registerScopeExtractor(ScopeExtractor extractor);

    /**
     * Extract scopes from a policy.
     *
     * @param policy        The input policy
     * @param policyContext The policy context
     * @return The set of scopes to use if succeeded, otherwise failure
     */
    Result<Set<String>> extractScopes(Policy policy, RequestPolicyContext policyContext);
}
