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

package org.eclipse.edc.policy.context.request;

import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractor;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractorRegistry;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequestScopeExtractorRegistryImpl implements RequestScopeExtractorRegistry {

    private final List<ExtractorEntry<?>> extractors = new ArrayList<>();

    @Override
    public <C extends RequestPolicyContext> void registerExtractor(Class<C> contextType, RequestScopeExtractor<C> extractor) {
        extractors.add(new ExtractorEntry<>(contextType, extractor));
    }

    @Override
    public <C extends RequestPolicyContext> Result<Set<String>> extractScopes(Policy policy, C context) {
        var scopes = new HashSet<String>();
        for (var entry : extractors) {
            if (entry.contextType().isAssignableFrom(context.getClass())) {
                @SuppressWarnings("unchecked")
                var extractor = (RequestScopeExtractor<C>) entry.extractor();
                var result = extractor.extractScopes(policy, context);
                if (result.failed()) {
                    return Result.failure(result.getFailureMessages());
                }
                scopes.addAll(result.getContent());
            }
        }
        return Result.success(scopes);
    }

    private record ExtractorEntry<C extends RequestPolicyContext>(Class<C> contextType, RequestScopeExtractor<C> extractor) {
    }
}
