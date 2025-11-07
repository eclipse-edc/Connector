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
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DcpScopeExtractorRegistry implements ScopeExtractorRegistry {

    private final List<ScopeExtractor> extractors = new ArrayList<>();

    @Override
    public void registerScopeExtractor(ScopeExtractor extractor) {
        extractors.add(extractor);
    }

    @Override
    public Result<Set<String>> extractScopes(Policy policy, RequestPolicyContext policyContext) {
        var visitor = new DcpScopeExtractorVisitor(extractors, policyContext);
        var policies = policy.accept(visitor);
        if (policyContext.hasProblems()) {
            return Result.failure(policyContext.getProblems());
        }
        return Result.success(policies);
    }

}
